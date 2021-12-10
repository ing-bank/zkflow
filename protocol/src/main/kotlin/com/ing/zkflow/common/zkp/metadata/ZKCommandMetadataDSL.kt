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
import java.util.function.Predicate
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
            mapping += commandMetadata.inputs.toZincTypes()
            mapping += commandMetadata.references.toZincTypes()
            mapping += commandMetadata.outputs.toZincTypes()
            return mapping.toMap()
        }

        private fun ContractStateTypeCountList.toZincTypes() = map { it.type to stateKClassToZincType(it.type) }

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
 * Describes the number of occurrences for a type.
 */
data class ContractStateTypeCount(val type: KClass<out ContractState>, val count: Int)

@ZKCommandMetadataDSL
class ContractStateTypeCountList : ArrayList<ContractStateTypeCount>() {
    infix fun Int.of(type: KClass<out ContractState>) = add(ContractStateTypeCount(type, this))
}

@ZKCommandMetadataDSL
class ZKCommandMetadata(val commandKClass: KClass<out CommandData>) {
    val commandSimpleName: String by lazy { commandKClass.simpleName ?: error("Command classes must be a named class") }

    /**
     * This is always true, and can't be changed
     */
    val networkParameters = true

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
     * This determines whether a circuit is expected to exist for this command.
     *
     * If false, ZKFLow will ignore this command for the ZKP circuit in all ways, except for Merkle tree calculation.
     */
    var private = false

    /**
     * Information on the circuit and related artifacts to be used.
     *
     * If the command is marked private, but this is null, ZKFLow will
     * try to find the circuit based on some default rules. If that fails,
     * an error is thrown.
     */
    var circuit: ZKCircuit? = null

    /**
     * Defines which transaction components should be "hidden" with ZKP
     */
    var zkFiltering: Predicate<Any> = Predicate { true }

    var numberOfSigners = 0

    val inputs = ContractStateTypeCountList()
    val references = ContractStateTypeCountList()
    val outputs = ContractStateTypeCountList()

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

    fun inputs(init: ContractStateTypeCountList.() -> Unit) = inputs.apply(init)
    fun references(init: ContractStateTypeCountList.() -> Unit) = references.apply(init)
    fun outputs(init: ContractStateTypeCountList.() -> Unit) = outputs.apply(init)

    /** Present when called, otherwise absent */
    fun timewindow() {
        timeWindow = true
    }

    fun resolved(): ResolvedZKCommandMetadata {
        return if (private) {
            PrivateResolvedZKCommandMetadata(
                circuit.resolve(this),
                zkFiltering,
                commandKClass,
                numberOfSigners,
                inputs.toList(),
                references.toList(),
                outputs.toList(),
                numberOfUserAttachments,
                timeWindow,
                notarySignatureScheme,
                participantSignatureScheme,
                attachmentConstraintType
            )
        } else {
            PublicResolvedZKCommandMetadata(
                commandKClass,
                numberOfSigners,
                inputs.toList(),
                references.toList(),
                outputs.toList(),
                numberOfUserAttachments,
                timeWindow,
                notarySignatureScheme,
                participantSignatureScheme,
                attachmentConstraintType
            )
        }
    }
}

fun ZKCommandData.commandMetadata(init: ZKCommandMetadata.() -> Unit): ResolvedZKCommandMetadata {
    return commandMetadata(this::class, init)
}

fun commandMetadata(commandKClass: KClass<out CommandData>, init: ZKCommandMetadata.() -> Unit): ResolvedZKCommandMetadata {
    return ZKCommandMetadata(commandKClass).apply(init).resolved()
}
