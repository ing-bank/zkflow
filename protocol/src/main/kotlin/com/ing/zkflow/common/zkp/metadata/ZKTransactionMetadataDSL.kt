package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
import com.ing.zkflow.common.zkp.ZKFlow.requireSupportedContractAttachmentConstraint
import com.ing.zkflow.common.zkp.ZKFlow.requireSupportedSignatureScheme
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.crypto.SignatureScheme
import net.corda.core.internal.objectOrNewInstance
import java.util.ServiceLoader
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberExtensionProperties
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubclassOf

@DslMarker
annotation class ZKTransactionMetadataDSL

@ZKTransactionMetadataDSL
data class ZKNotary(
    /**
     * The public key type used by the notary in this network.
     */
    var signatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
) {
    init {
        requireSupportedSignatureScheme(signatureScheme)
    }
}

@ZKTransactionMetadataDSL
open class ZKNetwork(
    var participantSignatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
    var attachmentConstraintType: KClass<out AttachmentConstraint> = DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT
) {
    var notary = ZKNotary()

    init {
        requireSupportedSignatureScheme(participantSignatureScheme)
        requireSupportedContractAttachmentConstraint(attachmentConstraintType)
    }

    fun notary(init: ZKNotary.() -> Unit) = notary.apply(init)
}

class DefaultZKNetwork : ZKNetwork()

@ZKTransactionMetadataDSL
class ZKCommandList : ArrayList<KClass<out CommandData>>() {
    class CommandNotObjectOrNoArgConstructorException(command: KClass<out CommandData>) :
        IllegalArgumentException("Command not supported: '$command'. $ERROR_COMMAND_NOT_OBJECT_OR_NOARG")

    class CommandNoMetadataFoundException(command: KClass<out CommandData>) :
        IllegalArgumentException("Command not supported: '$command'. $ERROR_COMMAND_NO_METADATA_FOUND")

    companion object {
        const val ERROR_COMMAND_NOT_UNIQUE = "Multiple commands of one type found. All commands in a ZKFLow transaction should be unique"
        const val ERROR_COMMAND_NOT_OBJECT_OR_NOARG =
            "ZKFlow only supports commands that are objects or that have a no arguments constructor"
        const val ERROR_COMMAND_NO_METADATA_FOUND = "No command metadata property found"
    }

    operator fun KClass<out CommandData>.unaryPlus() {
        require(!contains(this)) { ERROR_COMMAND_NOT_UNIQUE }
        add(this)
    }

    fun resolve(): List<ResolvedZKCommandMetadata> = map { kClass ->
        if (kClass.isSubclassOf(ZKCommandData::class)) {
            getMetadataFromMemberProperty(kClass)
        } else {
            /**
             * Metadata extension properties for non-ZKCommandData commands should be defined in
             * the companion object of one of the known commands in the command list.
             * That way, users can define metadata for commands that they use, without those commands having to support ZKFlow.
             */
            getMetadataFromExtensionProperty(kClass)
        }
    }

    private fun getMetadataFromMemberProperty(kClass: KClass<out CommandData>): ResolvedZKCommandMetadata {
        val command = getCommandInstance(kClass) as ZKCommandData
        return command.metadata
    }

    private fun getMetadataFromExtensionProperty(commandKClass: KClass<out CommandData>): ResolvedZKCommandMetadata {
        forEach { otherCommandKClass ->
            if (otherCommandKClass != commandKClass) {
                val metadataProperty = otherCommandKClass.companionObject?.declaredMemberExtensionProperties?.find { kProperty2 ->
                    kProperty2.name == ZKCommandData.METADATA_FIELD_NAME &&
                        kProperty2.returnType.classifier == ResolvedZKCommandMetadata::class &&
                        kProperty2.extensionReceiverParameter?.type?.classifier == commandKClass
                }
                val metadata =
                    metadataProperty
                        ?.getter?.call(
                            otherCommandKClass.companionObjectInstance,
                            getCommandInstance(commandKClass)
                        ) as? ResolvedZKCommandMetadata
                if (metadata != null) return metadata
            }
        }
        throw CommandNoMetadataFoundException(commandKClass)
    }

    /**
     * This instantiantes commands (again), causing the transaction metadata property itself to also be re-evaluated.
     * This causes a stack overflow. For this reason, we cache the resolved metadata.
     */
    private fun getCommandInstance(kClass: KClass<out CommandData>): CommandData {
        return try {
            kClass.objectOrNewInstance()
        } catch (e: IllegalArgumentException) {
            throw CommandNotObjectOrNoArgConstructorException(kClass)
        }
    }
}

@ZKTransactionMetadataDSL
class ZKTransactionMetadata {
    var network: ZKNetwork = DefaultZKNetwork()
    var commands: ZKCommandList = ZKCommandList()

    /**
     * The number of CorDapps required for the smart contract required for this transaction.
     * This number is determined by the location of all contracts required by this transaction.
     * They might be located in one CorDapp, or perhaps in multiple CorDapps.
     *
     * This number determines the number of ContractAttachments that will be attached to the transaction by Corda
     *
     * Example: if your transaction contains states from multiple contracts and these contracts are all part
     * of your own CorDapp. In this case the number of contract attachments would be 1.
     * Example: if your transaction contains states from both your own CorDapp's contracts and from
     * Corda's Tokens SDK contracts. In this case, the number of contract attachments would be 2.
     *
     * If not set, we will do a best guess based on the contracts found in the transaction
     */
    var numberOfCorDappsForContracts: Int? = null

    companion object {
        const val ERROR_COMMANDS_ALREADY_SET = "Commands already set"
        const val ERROR_NETWORK_ALREADY_SET = "Network already set"
    }

    fun network(init: ZKNetwork.() -> Unit): ZKNetwork {
        if (network !is DefaultZKNetwork) error(ERROR_NETWORK_ALREADY_SET)
        network = ZKNetwork().apply(init)
        return network
    }

    fun commands(init: ZKCommandList.() -> Unit): ZKCommandList {
        if (commands.isNotEmpty()) error(ERROR_COMMANDS_ALREADY_SET)
        commands = commands.apply(init)
        return commands
    }

    fun resolve(): ResolvedZKTransactionMetadata {
        return ResolvedZKTransactionMetadata(
            network,
            commands.resolve(),
            numberOfCorDappsForContracts
        )
    }
}

object TransactionMetadataCache {
    val resolvedTransactionMetadata = mutableMapOf<KClass<out ZKTransactionMetadataCommandData>, ResolvedZKTransactionMetadata>()

    fun findMetadataByCircuitName(circuitName: String): ResolvedZKTransactionMetadata {
        println("SEARCHING FOR: $circuitName")
        ServiceLoader.load(ZKTransactionMetadataCommandData::class.java).reload() // Just to be sure
        ServiceLoader.load(ZKTransactionMetadataCommandData::class.java).iterator().forEach { println("LOADED: ${it::class}") }

        return resolvedTransactionMetadata.entries.find { cacheEntry ->
            cacheEntry.value.commands.any { command ->
                command is PrivateResolvedZKCommandMetadata &&
                    command.commandKClass == cacheEntry.key &&
                    command.circuit.name == circuitName
            }
        }?.value ?: error("Could not find metadata for circuit with name $circuitName")
    }
}

class CachedResolvedTransactionMetadataDelegate(
    private val init: ZKTransactionMetadata.() -> Unit
) : ReadOnlyProperty<ZKTransactionMetadataCommandData, ResolvedZKTransactionMetadata> {

    override operator fun getValue(thisRef: ZKTransactionMetadataCommandData, property: KProperty<*>): ResolvedZKTransactionMetadata =
        TransactionMetadataCache.resolvedTransactionMetadata.getOrPut(thisRef::class) { ZKTransactionMetadata().apply(init).resolve() }
}

/**
 * Because ZKTransactionMetadata.resolve() instantiantes commands (again), the transaction metadata property itself
 * is also re-evaluated. This causes a stack overflow. For this reason, we cache the resolved metadata.
 * This may also enable us to later pre-fill the cache at compile-time.
 *
 * Original uncached: fun transactionMetadata(init: ZKTransactionMetadata.() -> Unit)  = ZKTransactionMetadata().apply(init).resolve()
 */
fun transactionMetadata(init: ZKTransactionMetadata.() -> Unit) = CachedResolvedTransactionMetadataDelegate(init)
