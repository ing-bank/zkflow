package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.annotations.corda.HashSize
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_DIGEST_IDENTIFIER
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_ATTACHMENT_CONSTRAINT_SIGNATURE_SCHEME
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
import com.ing.zkflow.common.zkp.ZKFlow.requireSupportedSignatureScheme
import com.ing.zkflow.common.zkp.metadata.ZKCircuit.Companion.resolve
import com.ing.zkflow.common.zkp.metadata.ZKNetwork.Companion.resolve
import com.ing.zkflow.crypto.IdentifyingDigestAlgorithm
import com.ing.zkflow.util.camelToSnakeCase
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
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
 * It contains information about locations, artifacts, etc., so that
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

        private fun List<ZKTypedElement>.toZincTypes() = map { it.type to stateKClassToZincType(it.type) }

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
        @SuppressFBWarnings("PATH_TRAVERSAL_IN", justification = "False positive: folders are calculated")
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

@ZKCommandMetadataDSL
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

@ZKCommandMetadataDSL
data class ZKNetwork(
    var participantSignatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
    var digestService: IdentifyingDigestAlgorithm = DEFAULT_ZKFLOW_DIGEST_IDENTIFIER
) {
    var notary = ZKNotary()
    fun notary(init: ZKNotary.() -> Unit) = notary.apply(init)

    /**
     * One may specify attachment constraint either by setting the class directly if such constraint requires no extra parameters,
     * or by invoking an appropriate function.
     */
    private var zkAttachmentConstraintType: ZKAttachmentConstraint = ZKAttachmentConstraint.default
    // This field has a utility function to improve UX and won't be used for resolution.
    var attachmentConstraintType: KClass<out AttachmentConstraint> = ZKAttachmentConstraint.default.kClass
        set(value) {
            when (value) {
                SignatureAttachmentConstraint::class -> error("To modify `${SignatureAttachmentConstraint::class.qualifiedName}` use `signatureAttachmentConstraint { ... }`")
                HashAttachmentConstraint::class -> error("To modify `${HashAttachmentConstraint::class.qualifiedName}` use `hashAttachmentConstraint { ... }`")
                else -> {
                    field = value
                    zkAttachmentConstraintType = ZKAttachmentConstraint.OnlyType(value)
                }
            }
        }
    fun signatureAttachmentConstraint(init: ZKAttachmentConstraint.SignatureAttachment.() -> Unit): ZKAttachmentConstraint {
        zkAttachmentConstraintType = ZKAttachmentConstraint.SignatureAttachment().apply(init)
        return zkAttachmentConstraintType
    }
    fun hashAttachmentConstraint(init: ZKAttachmentConstraint.HashAttachment.() -> Unit): ZKAttachmentConstraint {
        zkAttachmentConstraintType = ZKAttachmentConstraint.HashAttachment().apply(init)
        return zkAttachmentConstraintType
    }

    companion object {
        fun ZKNetwork?.resolve(): ResolvedZKNetwork {
            return if (this == null) ResolvedZKNetwork(
                DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
                ZKAttachmentConstraint.default,
                DEFAULT_ZKFLOW_DIGEST_IDENTIFIER,
                ZKNotary()
            )
            else
                ResolvedZKNetwork(participantSignatureScheme, zkAttachmentConstraintType, digestService, notary)
        }
    }
}

@ZKCommandMetadataDSL
sealed class ZKAttachmentConstraint private constructor(val kClass: KClass<out AttachmentConstraint>) {
    class SignatureAttachment(var signatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_ATTACHMENT_CONSTRAINT_SIGNATURE_SCHEME) : ZKAttachmentConstraint(SignatureAttachmentConstraint::class)
    class HashAttachment(algorithm: KClass<out Annotation> = DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM) : ZKAttachmentConstraint(HashAttachmentConstraint::class) {
        internal var hashLength: Int = deduceHashLength(algorithm)
        var algorithm: KClass<out Annotation> = algorithm
            set(value) {
                hashLength = deduceHashLength(value)
                field = value
            }

        private fun deduceHashLength(algorithm: KClass<out Annotation>): Int {
            val hashSizeAnnotations = algorithm.annotations.filterIsInstance<HashSize>()
            when (hashSizeAnnotations.size) {
                0 -> error("Hash class `${algorithm.qualifiedName}` must have a `${HashSize::class.qualifiedName}` annotation")
                1 -> return hashSizeAnnotations.single().size
                else -> error("Hash class `${algorithm.qualifiedName}` must have a _single_ `${HashSize::class.qualifiedName}` annotation")
            }
        }
    }

    class OnlyType(attachmentConstraintType: KClass<out AttachmentConstraint>) : ZKAttachmentConstraint(attachmentConstraintType)

    companion object {
        val default = when (DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT) {
            SignatureAttachmentConstraint::class -> SignatureAttachment()
            HashAttachmentConstraint::class -> HashAttachment()
            else -> OnlyType(DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT)
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

interface ZKTypedElement {
    val type: KClass<out ContractState>
}

/**
 * Describes the StateRefs (inputs or references) contents of which that should be available inside ZKP circuit
 */
data class ZKReference(override val type: KClass<out ContractState>, val forcePrivate: Boolean, val index: Int) : ZKTypedElement

/**
 * Describes the private component at a certain index in transaction's component list.
 */
data class ZKProtectedComponent(override val type: KClass<out ContractState>, val private: Boolean, val index: Int) : ZKTypedElement

@ZKCommandMetadataDSL
class ZKReferenceList : ArrayList<ZKReference>() {

    fun private(type: KClass<out ContractState>): Pair<KClass<out ContractState>, Boolean> = type to true
    fun any(type: KClass<out ContractState>): Pair<KClass<out ContractState>, Boolean> = type to false

    infix fun Pair<KClass<out ContractState>, Boolean>.at(index: Int) = add(ZKReference(this.first, this.second, index))

    override fun add(element: ZKReference): Boolean {
        if (any { it.index == element.index }) error("Component visibility is already set for index ${element.index}")
        return super.add(element)
    }
}

@ZKCommandMetadataDSL
class ZKProtectedComponentList : ArrayList<ZKProtectedComponent>() {

    fun public(type: KClass<out ContractState>): Pair<KClass<out ContractState>, Boolean> = type to false
    fun private(type: KClass<out ContractState>): Pair<KClass<out ContractState>, Boolean> = type to true

    infix fun Pair<KClass<out ContractState>, Boolean>.at(index: Int) = add(ZKProtectedComponent(this.first, this.second, index))

    override fun add(element: ZKProtectedComponent): Boolean {
        if (any { it.index == element.index }) error("Component visibility is already set for index ${element.index}")
        return super.add(element)
    }
}

/**
 * Only ZK-Protected (i.e. Private or Mixed) components should be listed here.
 * Components that are not mentioned here are considered Public by default and will be publicly visible.
 */
@ZKCommandMetadataDSL
class ZKCommandMetadata(val commandKClass: KClass<out CommandData>) {
    val commandSimpleName: String by lazy { commandKClass.simpleName ?: error("Command classes must be a named class") }

    /**
     * Information on the circuit and related artifacts to be used.
     *
     * If the command is marked private, but this is null, ZKFLow will
     * try to find the circuit based on some default rules. If that fails,
     * an error is thrown.
     */
    var circuit: ZKCircuit? = null

    var zkNetwork: ZKNetwork? = null

    var numberOfSigners = 0

    val privateInputs = ZKReferenceList()
    val privateReferences = ZKReferenceList()
    val privateOutputs = ZKProtectedComponentList()

    var timeWindow = false

    fun circuit(init: ZKCircuit.() -> Unit): ZKCircuit {
        circuit = ZKCircuit().apply(init)
        return circuit!!
    }

    fun network(init: ZKNetwork.() -> Unit): ZKNetwork {
        zkNetwork = ZKNetwork().apply(init)
        return zkNetwork!!
    }

    fun inputs(init: ZKReferenceList.() -> Unit) = privateInputs.apply(init)
    fun references(init: ZKReferenceList.() -> Unit) = privateReferences.apply(init)
    fun outputs(init: ZKProtectedComponentList.() -> Unit) = privateOutputs.apply(init)

    fun resolved(): ResolvedZKCommandMetadata =
        ResolvedZKCommandMetadata(
            circuit.resolve(this),
            commandKClass,
            numberOfSigners,
            privateInputs.toList(),
            privateReferences.toList(),
            privateOutputs.toList(),
            timeWindow,
            zkNetwork.resolve()
        )
}

fun ZKCommandData.commandMetadata(init: ZKCommandMetadata.() -> Unit): ResolvedZKCommandMetadata {
    return commandMetadata(this::class, init)
}

fun commandMetadata(commandKClass: KClass<out CommandData>, init: ZKCommandMetadata.() -> Unit): ResolvedZKCommandMetadata {
    return ZKCommandMetadata(commandKClass).apply(init).resolved()
}
