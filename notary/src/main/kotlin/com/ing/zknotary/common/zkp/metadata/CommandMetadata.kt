package com.ing.zknotary.common.zkp.metadata

import com.ing.zknotary.common.zkp.ZKFlow.DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT
import com.ing.zknotary.common.zkp.ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
import com.ing.zknotary.common.zkp.ZKFlow.requireSupportedSignatureScheme
import com.ing.zknotary.common.zkp.metadata.ZKCircuit.Companion.resolve
import com.ing.zknotary.gradle.zinc.template.TemplateParameters.Companion.camelToSnakeCase
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
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
        private val DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH = "${System.getProperty("user.dir")}/build/zinc/commands/"

        private fun javaClass2ZincType(commandMetadata: ZKCommandMetadata): Map<KClass<*>, ZincType> {
            val mapping = mutableListOf<Pair<KClass<*>, ZincType>>()
            mapping += commandMetadata.inputs.toZincTypes()
            mapping += commandMetadata.references.toZincTypes()
            mapping += commandMetadata.outputs.toZincTypes()
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
                name = commandMetadata.commandSimpleName,
                buildFolder = File(DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH + commandMetadata.commandSimpleName),
                buildTimeout = DEFAULT_BUILD_TIMEOUT,
                setupTimeout = DEFAULT_SETUP_TIMEOUT,
                provingTimeout = DEFAULT_PROVING_TIMEOUT,
                verificationTimeout = DEFAULT_VERIFICATION_TIMEOUT
            )

            return ResolvedZKCircuit(
                commandKClass = commandMetadata.commandKClass,
                javaClass2ZincType = javaClass2ZincType(commandMetadata),
                name = name ?: commandMetadata.commandSimpleName,
                buildFolder = buildFolder ?: File(DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH + name),
                buildTimeout = buildTimeout ?: DEFAULT_BUILD_TIMEOUT,
                setupTimeout = setupTimeout ?: DEFAULT_SETUP_TIMEOUT,
                provingTimeout = provingTimeout ?: DEFAULT_PROVING_TIMEOUT,
                verificationTimeout = verificationTimeout ?: DEFAULT_VERIFICATION_TIMEOUT
            )
        }
    }
}

data class ResolvedZKCircuit(
    val commandKClass: KClass<out CommandData>,
    var name: String,
    /**
     * Unless provided, this will be calculated to be `<gradle module>/src/main/zinc/<transaction.name>/commands/<command.name>`
     * This is where the circuit elements for this command can be found
     */
    val buildFolder: File,
    val javaClass2ZincType: Map<KClass<*>, ZincType>,
    val buildTimeout: Duration,
    val setupTimeout: Duration,
    val provingTimeout: Duration,
    val verificationTimeout: Duration
)

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

    fun inputs(init: TypeCountList.() -> Unit) = inputs.apply(init)
    fun references(init: TypeCountList.() -> Unit) = references.apply(init)
    fun outputs(init: TypeCountList.() -> Unit) = outputs.apply(init)

    /** Present when called, otherwise absent */
    fun timewindow() {
        timeWindow = true
    }

    val resolved: ResolvedZKCommandMetadata by lazy {
        if (private) {
            PrivateResolvedZKCommandMetadata(
                circuit.resolve(this),
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

fun CommandData.commandMetadata(init: ZKCommandMetadata.() -> Unit): ZKCommandMetadata {
    return ZKCommandMetadata(this::class).apply(init)
}

interface ResolvedCommandMetadata {
    val commandKClass: KClass<out CommandData>
    val commandSimpleName: String

    /**
     * The notary [SignatureScheme] type required by this command.
     *
     * This should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    val notarySignatureScheme: SignatureScheme

    /**
     * The participant [SignatureScheme] type required by this command.
     *
     * Due to current limitations of the ZKP command, only one [SignatureScheme] per command is allowed for transaction participants.
     * This should be enforced at network level and therefore should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    val participantSignatureScheme: SignatureScheme

    /**
     * The attachment constraint required by this command for all states
     *
     * Due to current limitations of the ZKP command, only one [AttachmentConstraint] per transaction is allowed.
     * This should be enforced at network level and therefore should match the [AttachmentConstraint] defined for the network
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    val attachmentConstraintType: KClass<out AttachmentConstraint>

    val numberOfSigners: Int

    val inputs: List<TypeCount>
    val references: List<TypeCount>
    val outputs: List<TypeCount>
    val numberOfUserAttachments: Int
    val timeWindow: Boolean

    /**
     * This is always true, and can't be changed
     */
    val networkParameters: Boolean

    /**
     * The list of all contract Class names used by all states in this command
     */
    val contractClassNames: List<ContractClassName>
}

/**
 * Obtain the typename of the required [ContractClass] associated with the target [ContractState], using the
 * [BelongsToContract] annotation by default, but falling through to checking the state's enclosing class if there is
 * one and it inherits from [Contract].
 */
val KClass<out ContractState>.requiredContractClassName: String?
    get() {
        val annotation = java.getAnnotation(BelongsToContract::class.java)
        if (annotation != null) {
            return annotation.value.java.typeName
        }
        val enclosingClass = java.enclosingClass ?: return null
        return if (Contract::class.java.isAssignableFrom(enclosingClass)) enclosingClass.typeName else null
    }

abstract class ResolvedZKCommandMetadata(
    final override val notarySignatureScheme: SignatureScheme,
    final override val participantSignatureScheme: SignatureScheme,
    final override val attachmentConstraintType: KClass<out AttachmentConstraint>
) : ResolvedCommandMetadata {
    override val networkParameters = true
    override val commandSimpleName: String by lazy { commandKClass.simpleName ?: error("Command classes must be a named class") }
    override val contractClassNames: List<ContractClassName>
        get() {
            val stateTypes = (inputs.flattened + outputs.flattened + references.flattened).distinct()
            return stateTypes.map {
                requireNotNull(it.requiredContractClassName) {
                    "Unable to infer Contract class name because state class $it is not annotated with " +
                        "@BelongsToContract, and does not have an enclosing class which implements Contract."
                }
            }
        }

    init {
        requireSupportedSignatureScheme(participantSignatureScheme)
        requireSupportedSignatureScheme(notarySignatureScheme)
    }
}

@Suppress("LongParameterList") // param length caused by Corda component count
class PrivateResolvedZKCommandMetadata(
    /**
     * Infomation on the circuit and related artifacts to be used.
     */
    val circuit: ResolvedZKCircuit,
    override val commandKClass: KClass<out CommandData>,
    override val numberOfSigners: Int,
    override val inputs: List<TypeCount>,
    override val references: List<TypeCount>,
    override val outputs: List<TypeCount>,
    override val numberOfUserAttachments: Int,
    override val timeWindow: Boolean,
    notarySignatureScheme: SignatureScheme,
    participantSignatureScheme: SignatureScheme,
    attachmentConstraintType: KClass<out AttachmentConstraint>,
) : ResolvedZKCommandMetadata(notarySignatureScheme, participantSignatureScheme, attachmentConstraintType)

@Suppress("LongParameterList") // param length caused by Corda component count
class PublicResolvedZKCommandMetadata(
    override val commandKClass: KClass<out CommandData>,
    override val numberOfSigners: Int,
    override val inputs: List<TypeCount>,
    override val references: List<TypeCount>,
    override val outputs: List<TypeCount>,
    override val numberOfUserAttachments: Int,
    override val timeWindow: Boolean,
    notarySignatureScheme: SignatureScheme,
    participantSignatureScheme: SignatureScheme,
    attachmentConstraintType: KClass<out AttachmentConstraint>,
) : ResolvedZKCommandMetadata(notarySignatureScheme, participantSignatureScheme, attachmentConstraintType)
