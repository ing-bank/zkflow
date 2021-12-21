package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
import com.ing.zkflow.common.zkp.ZKFlow.requireSupportedSignatureScheme
import com.ing.zkflow.common.zkp.metadata.ZKCircuit.Companion.resolve
import com.ing.zkflow.util.camelToSnakeCase
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SignatureScheme
import net.corda.core.utilities.hours
import net.corda.core.utilities.minutes
import net.corda.core.utilities.seconds
import java.io.File
import java.time.Duration
import kotlin.reflect.KClass

@DslMarker
annotation class ZKCommandMetadataDSL

/**
 * This class describes the circuit associated  with this command.
 *
 * It containts information about locations, artifacts, etc., so that
 * ZKFLow knows how to use it.
 */
@ZKCommandMetadataDSL
data class ZKCircuit(
    /**
     * This name can be anything.
     * If null, it will be derived from the command name.
     */
    var name: String? = null,
    var buildFolder: File? = null,
    var buildTimeout: Duration? = null,
    var setupTimeout: Duration? = null,
    var provingTimeout: Duration? = null,
    var verificationTimeout: Duration? = null
) {
    companion object {
        private val DEFAULT_BUILD_TIMEOUT = 15.seconds
        private val DEFAULT_SETUP_TIMEOUT = 2.hours
        private val DEFAULT_PROVING_TIMEOUT = 5.minutes
        private val DEFAULT_VERIFICATION_TIMEOUT = 3.seconds
        private val DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH = "${System.getProperty("user.dir")}/build/zinc/"

        private fun javaClass2ZincType(commandMetadata: ZKCommandMetadata): Map<KClass<out ContractState>, ZincType> {
            val mapping = mutableListOf<Pair<KClass<out ContractState>, ZincType>>()
            mapping += commandMetadata.privateInputs.toZincTypes()
            mapping += commandMetadata.privateReferences.toZincTypes()
            mapping += commandMetadata.privateOutputs.toZincTypes()
            return mapping.toMap()
        }

        private fun ZKProtectedComponentList.toZincTypes() = map { it.type to stateKClassToZincType(it.type) }

        private fun stateKClassToZincType(kClass: KClass<out ContractState>): ZincType {
            val simpleName = kClass.simpleName ?: error("classes used in transactions must be a named class")
            return ZincType(
                simpleName,
                simpleName.camelToSnakeCase() + ".zn"
            )
        }

        /**
         * If the circuit information is not provided, set defaults for the circuit information.
         *
         * This is for the scenario where a command is marked private, but there is no circuit info provided.
         * In that case, sane defaults are used.
         *
         * Option 1: private = true, circuit info is provided
         * Option 2: private = true, circuit info is NOT provided.
         */
        fun ZKCircuit?.resolve(commandMetadata: ZKCommandMetadata): ResolvedZKCircuit {
            if (this == null) return ResolvedZKCircuit(
                commandKClass = commandMetadata.commandKClass,
                javaClass2ZincType = javaClass2ZincType(commandMetadata),
                name = commandMetadata.commandSimpleName.camelToSnakeCase(),
                buildFolder = File(DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH + commandMetadata.commandSimpleName.camelToSnakeCase()),
                buildTimeout = DEFAULT_BUILD_TIMEOUT,
                setupTimeout = DEFAULT_SETUP_TIMEOUT,
                provingTimeout = DEFAULT_PROVING_TIMEOUT,
                verificationTimeout = DEFAULT_VERIFICATION_TIMEOUT
            )

            return ResolvedZKCircuit(
                commandKClass = commandMetadata.commandKClass,
                javaClass2ZincType = javaClass2ZincType(commandMetadata),
                name = name ?: commandMetadata.commandSimpleName.camelToSnakeCase(),
                buildFolder = buildFolder ?: File(DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH + name),
                buildTimeout = buildTimeout ?: DEFAULT_BUILD_TIMEOUT,
                setupTimeout = setupTimeout ?: DEFAULT_SETUP_TIMEOUT,
                provingTimeout = provingTimeout ?: DEFAULT_PROVING_TIMEOUT,
                verificationTimeout = verificationTimeout ?: DEFAULT_VERIFICATION_TIMEOUT
            )
        }
    }
}

/**
 * Describes a Zinc type
 *
 * Most likely used in a mapping from Zinc type to Kotlin type, used for Zinc code generation
 */
data class ZincType(
    /**
     * The name of the type as found in (generated) Zinc
     */
    val typeName: String,
    /**
     * The file where this type is defined
     */
    val fileName: String
)

/**
 * Defines a component visibility inside transaction.
 * "Public" components are plaintext and visible to everybody during tx creation and backchain validation, but not available inside ZKP circuit due to performance optimization.
 * "Private" components are shielded by ZKP and only visible to transaction participants, they are available inside ZKP circuit but not during backchain validation.
 * "Mixed" components are visible both in ZKP circuit and plaintext transaction, they are used when we essentially public components are checked inside ZKP circuit.
 * For now we only use "Private" and "Mixed" in our DSL.
 */
enum class ZkpVisibility {
    Public, Private, Mixed
}

/**
 * Describes the private component at a certain index in transaction's component list.
 */
data class ZKProtectedComponent(val type: KClass<out ContractState>, val visibility: ZkpVisibility, val index: Int)

@ZKCommandMetadataDSL
class ZKProtectedComponentList : ArrayList<ZKProtectedComponent>() {
    infix fun Int.private(type: KClass<out ContractState>) = add(ZKProtectedComponent(type, ZkpVisibility.Private, this))
    infix fun Int.mixed(type: KClass<out ContractState>) = add(ZKProtectedComponent(type, ZkpVisibility.Mixed, this))
}

/**
 * Only ZK-Protected (i.e. Private or Mixed) components should be listed here.
 * Components that are not mentioned here are considered Public by default and will be publicly visible.
 */
@ZKCommandMetadataDSL
class ZKCommandMetadata(val commandKClass: KClass<out CommandData>) {
    val commandSimpleName: String by lazy { commandKClass.simpleName ?: error("Command classes must be a named class") }

    /**
     * The notary [SignatureScheme] type required by this command.
     *
     * This should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    var notarySignatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    /**
     * The participant [SignatureScheme] type required by this command.
     *
     * Due to current limitations of the ZKP command, only one [SignatureScheme] per command is allowed for transaction participants.
     * This should be enforced at network level and therefore should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    var participantSignatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    /**
     * The attachment constraint required by this command for all states
     *
     * Due to current limitations of the ZKP command, only one [AttachmentConstraint] per transaction is allowed.
     * This should be enforced at network level and therefore should match the [AttachmentConstraint] defined for the network
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    var attachmentConstraintType: KClass<out AttachmentConstraint> = DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT

    /**
     * Information on the circuit and related artifacts to be used.
     *
     * If the command is marked private, but this is null, ZKFLow will
     * try to find the circuit based on some default rules. If that fails,
     * an error is thrown.
     */
    var circuit: ZKCircuit? = null

    var numberOfSigners = 0

    val privateInputs = ZKProtectedComponentList()
    val privateReferences = ZKProtectedComponentList()
    val privateOutputs = ZKProtectedComponentList()

    /**
     * These are only the attachments the user explicitly adds themselves.
     *
     * Contract attachments and other default attachments are added at transaction metadata level.
     */
    var numberOfUserAttachments = 0

    var timeWindow = false

    init {
        requireSupportedSignatureScheme(participantSignatureScheme)
        requireSupportedSignatureScheme(notarySignatureScheme)
    }

    fun circuit(init: ZKCircuit.() -> Unit): ZKCircuit {
        circuit = ZKCircuit().apply(init)
        return circuit!!
    }

    fun privateInputs(init: ZKProtectedComponentList.() -> Unit) = privateInputs.apply(init)
    fun privateReferences(init: ZKProtectedComponentList.() -> Unit) = privateReferences.apply(init)
    fun privateOutputs(init: ZKProtectedComponentList.() -> Unit) = privateOutputs.apply(init)

    fun resolved(): ResolvedZKCommandMetadata =
        PrivateResolvedZKCommandMetadata(
            circuit.resolve(this),
            commandKClass,
            numberOfSigners,
            privateInputs.toList(),
            privateReferences.toList(),
            privateOutputs.toList(),
            numberOfUserAttachments,
            timeWindow,
            notarySignatureScheme,
            participantSignatureScheme,
            attachmentConstraintType
        )
}

fun ZKCommandData.commandMetadata(init: ZKCommandMetadata.() -> Unit): ResolvedZKCommandMetadata {
    return commandMetadata(this::class, init)
}

fun commandMetadata(commandKClass: KClass<out CommandData>, init: ZKCommandMetadata.() -> Unit): ResolvedZKCommandMetadata {
    return ZKCommandMetadata(commandKClass).apply(init).resolved()
}
