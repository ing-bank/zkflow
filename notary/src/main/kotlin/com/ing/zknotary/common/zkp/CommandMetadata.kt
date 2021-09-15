package com.ing.zknotary.common.zkp

import com.ing.zknotary.gradle.zinc.template.TemplateParameters.Companion.camelToSnakeCase
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
    val commandMetadata: ZKCommandMetadata,
    /**
     * This name can be anything.
     * If  null, it will be derived from the command name.
     */
    var name: String? = null,
    val buildFolder: File? = null,
    val buildTimeout: Duration? = null,
    val setupTimeout: Duration? = null,
    val provingTimeout: Duration? = null,
    val verificationTimeout: Duration? = null
)

data class ResolvedZKCircuit(
    val commandKClass: KClass<out CommandData>,
    var name: String,
    /**
     * Unless provided, this will be calculated to be `<gradle module>/src/main/zinc/<circuit.name>`
     */
    val buildFolder: File,
    val javaClass2ZincType: Map<KClass<*>, ZincType>,
    val buildTimeout: Duration,
    val setupTimeout: Duration,
    val provingTimeout: Duration,
    val verificationTimeout: Duration
) {
    companion object {
        private val DEFAULT_BUILD_TIMEOUT = 15.seconds
        private val DEFAULT_SETUP_TIMEOUT = 2.hours
        private val DEFAULT_PROVING_TIMEOUT = 5.minutes
        private val DEFAULT_VERIFICATION_TIMEOUT = 3.seconds
        private val DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH = "${System.getProperty("user.dir")}/build/zinc/"

        fun resolve(circuit: ZKCircuit?): ResolvedZKCircuit? {
            if (circuit == null) return null
            return ResolvedZKCircuit(
                commandKClass = circuit.commandMetadata.commandKClass,
                name = circuit.name ?: circuit.commandMetadata.commandKClass.simpleName ?: error("Command classes must be a named class"),
                buildFolder = circuit.buildFolder ?: File(DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH + circuit.name),
                javaClass2ZincType = javaClass2ZincType(circuit.commandMetadata),
                buildTimeout = circuit.buildTimeout ?: DEFAULT_BUILD_TIMEOUT,
                setupTimeout = circuit.setupTimeout ?: DEFAULT_SETUP_TIMEOUT,
                provingTimeout = circuit.provingTimeout ?: DEFAULT_PROVING_TIMEOUT,
                verificationTimeout = circuit.verificationTimeout ?: DEFAULT_VERIFICATION_TIMEOUT
            )
        }

        private fun javaClass2ZincType(commandMetadata: ZKCommandMetadata): Map<KClass<*>, ZincType> {
            val mapping = mutableListOf<Pair<KClass<*>, ZincType>>()
            mapping += commandMetadata.inputs.toZincTypes()
            mapping += commandMetadata.references.toZincTypes()
            mapping += commandMetadata.outputs.toZincTypes()
            mapping += commandMetadata.userAttachments.toZincTypes()
            return mapping.toMap()
        }

        private fun TypeCountList.toZincTypes() = map { it.type to kClassToZincType(it.type) }

        private fun kClassToZincType(kClass: KClass<*>): ZincType {
            val simpleName = kClass.simpleName ?: error("classes used in transactins must be a named class")
            return ZincType(
                simpleName,
                simpleName.camelToSnakeCase()
            )
        }
    }
}

data class ZincType(
    val typeName: String,
    val fileName: String
)

/**
 * Describes the number of occurrences for a type.
 */
data class TypeCount(val type: KClass<out ContractState>, val count: Int)

@ZKCommandMetadataDSL
class TypeCountList : ArrayList<TypeCount>() {
    infix fun Int.of(type: KClass<out ContractState>) = add(TypeCount(type, this))
}

@ZKCommandMetadataDSL
class ZKCommandMetadata(val commandKClass: KClass<out CommandData>) {
    /**
     * This is always true, and can't be changed
     */
    val networkParameters = true

    /**
     * The notary [SignatureScheme] type required by this circuit.
     *
     * This should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    var notarySignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    /**
     * The participant [SignatureScheme] type required by this circuit.
     *
     * Due to current limitations of the ZKP circuit, only one [SignatureScheme] per circuit is allowed for transaction participants.
     * This should be enforced at network level and therefore should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    var participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    /**
     * This determines whether a circuit is expected to exist for this command.
     *
     * If false, ZKFLow will ignore this command for the ZKP circuit in all ways, except for Merkle tree calculation.
     */
    var private = false

    /**
     * Infomation on the circuit and related artifacts to be used.
     *
     * If the command is marked private, but this is null, ZKFLow will
     * try to find the circuit based on some default rules. If that fails,
     * an error is thrown.
     */
    var circuit: ZKCircuit? = null

    var numberOfSigners = 0

    val inputs = TypeCountList()
    val references = TypeCountList()
    val outputs = TypeCountList()
    val userAttachments = TypeCountList()
    var timeWindow = false

    init {
        ZKFlow.requireSupportedSignatureScheme(participantSignatureScheme)
        ZKFlow.requireSupportedSignatureScheme(notarySignatureScheme)
    }

    fun circuit(init: ZKCircuit.() -> Unit): ZKCircuit {
        circuit = ZKCircuit(this).apply(init)
        return circuit!!
    }

    fun inputs(init: TypeCountList.() -> Unit) = inputs.apply(init)
    fun references(init: TypeCountList.() -> Unit) = references.apply(init)
    fun outputs(init: TypeCountList.() -> Unit) = outputs.apply(init)

    /**
     * These are only the attachments the user explicitly adds themselves.
     *
     * Contract attachments and other default attachments are added automatically
     * and calculated based on the number of contracts in the transaction.
     */
    fun userAttachments(init: TypeCountList.() -> Unit) = userAttachments.apply(init)

    /** Present when called, otherwise absent */
    fun timewindow() {
        timeWindow = true
    }

    fun resolve(): ResolvedZKCommandMetadata {

        return ResolvedZKCommandMetadata(
            commandKClass,
            notarySignatureScheme,
            participantSignatureScheme,
            private,
            ResolvedZKCircuit.resolve(circuit),
            numberOfSigners,
            inputs.toList(),
            references.toList(),
            outputs.toList(),
            userAttachments.toList(),
            timeWindow
        )
    }
}

fun CommandData.commandMetadata(init: ZKCommandMetadata.() -> Unit): ZKCommandMetadata {
    return ZKCommandMetadata(this::class).apply(init)
}

data class ResolvedZKCommandMetadata(
    val commandKClass: KClass<out CommandData>,

    /**
     * The notary [SignatureScheme] type required by this circuit.
     *
     * This should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    val notarySignatureScheme: SignatureScheme,

    /**
     * The participant [SignatureScheme] type required by this circuit.
     *
     * Due to current limitations of the ZKP circuit, only one [SignatureScheme] per circuit is allowed for transaction participants.
     * This should be enforced at network level and therefore should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    val participantSignatureScheme: SignatureScheme,

    /**
     * This determines whether a circuit is expected to exist for this command.
     *
     * If false, ZKFLow will ignore this command for the ZKP circuit in all ways, except for Merkle tree calculation.
     */
    val private: Boolean,

    /**
     * Infomation on the circuit and related artifacts to be used.
     *
     * If the command is marked private, but this is null, ZKFLow will
     * try to find the circuit based on some default rules. If that fails,
     * an error is thrown.
     */
    val circuit: ResolvedZKCircuit?,

    val numberOfSigners: Int,

    val inputs: List<TypeCount>,
    val references: List<TypeCount>,
    val outputs: List<TypeCount>,
    val userAttachments: List<TypeCount>,
    val timeWindow: Boolean
) {
    /**
     * This is always true, and can't be changed
     */
    val networkParameters = true

    init {
        ZKFlow.requireSupportedSignatureScheme(participantSignatureScheme)
        ZKFlow.requireSupportedSignatureScheme(notarySignatureScheme)
    }
}

// class PrivateResolvedZKCommandMetadata() : ResolvedZKCommandMetadata()