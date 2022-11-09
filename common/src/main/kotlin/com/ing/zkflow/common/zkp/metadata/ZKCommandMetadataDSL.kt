package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.zkp.metadata.ZKCircuit.Companion.resolve
import com.ing.zkflow.util.camelToSnakeCase
import com.ing.zkflow.util.hasAnnotation
import com.ing.zkflow.util.scopedName
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.corda.core.contracts.ContractState
import net.corda.core.utilities.hours
import net.corda.core.utilities.minutes
import net.corda.core.utilities.seconds
import java.io.File
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@DslMarker
annotation class ZKCommandMetadataDSL

/**
 * This class describes the circuit associated  with this command.
 *
 * It contains information about locations, artifacts, etc., so that
 * ZKFlow knows how to use it.
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
                name = commandMetadata.scopedCommandName.camelToSnakeCase(),
                buildFolder = File(DEFAULT_CIRCUIT_BUILD_FOLDER_PARENT_PATH + commandMetadata.scopedCommandName.camelToSnakeCase()),
                buildTimeout = DEFAULT_BUILD_TIMEOUT,
                setupTimeout = DEFAULT_SETUP_TIMEOUT,
                provingTimeout = DEFAULT_PROVING_TIMEOUT,
                verificationTimeout = DEFAULT_VERIFICATION_TIMEOUT
            )

            return ResolvedZKCircuit(
                commandKClass = commandMetadata.commandKClass,
                name = name ?: commandMetadata.scopedCommandName.camelToSnakeCase(),
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
 * Describes the inputs and references that should be available inside ZKP circuit and if it should be forced to be private.
 */
data class ZKReference(override val type: KClass<out ContractState>, override val index: Int, internal val forcePrivate: Boolean) :
    ZKIndexedTypedElement {
    init {
        require(type.isSubclassOf(VersionedContractStateGroup::class)) { "Input or reference types must implement `VersionedContractStateGroup`" }
        require(type.hasAnnotation<ZKP>()) { "Input or reference types must be annotated with `@ZKP`" }
    }

    override fun mustBePrivate() = forcePrivate
}

/**
 * Describes an output ContractState at a certain index in transaction's component list and whether it should be private to the ZKP circuit or also
 * visible in the transaction Merkle tree.
 */
data class ZKProtectedComponent(override val type: KClass<out ContractState>, override val index: Int, internal val private: Boolean) :
    ZKIndexedTypedElement {
    init {
        require(type.isSubclassOf(VersionedContractStateGroup::class)) { "Input or reference types must implement `VersionedContractStateGroup`" }
        require(type.hasAnnotation<ZKP>()) { "Input or reference types must be annotated with `@ZKP`" }
    }

    override fun mustBePrivate() = private
}

/**
 * A list of input or references UTXOs that this ZKCommand needs to have access to.
 *
 * When building a [WireTransaction], this information is used to determine whether it
 * should be enforced that for each [StateRef], its UTXO was private in the transaction that created it.
 * For each StateRef there are three options:
 * - private: the UTXO will NOT be allowed to be public in the transaction that created it.
 * - any: the UTXO WILL be allowed to be public in the transaction that created it.
 * - not mentioned at all: the UTXO WILL be allowed to be public in the transaction that created it.
 *
 * When building a [ZKVerifierTransaction], this information is used to determine which UTXOs
 * should be present in the witness. For each UTXO there are three options:
 * - private: the UTXO is: present in the witness
 * - any: the UTXO is present in the witness
 * - not mentioned at all: the UTXO is NOT present in the witness
 */
@ZKCommandMetadataDSL
class ZKReferenceList : ArrayList<ZKReference>() {
    /**
     * The UTXO for a `private` reference is present in the witness.
     * That UTXO will NOT be allowed to be public in the transaction that created it.
     */
    fun private(type: KClass<out ContractState>): Pair<KClass<out ContractState>, Boolean> = type to true

    /**
     * The UTXO for an `any` reference is present in the witness.
     * That UTXO WILL be allowed to be public in the transaction that created it.
     */
    fun any(type: KClass<out ContractState>): Pair<KClass<out ContractState>, Boolean> = type to false

    infix fun Pair<KClass<out ContractState>, Boolean>.at(index: Int) = add(ZKReference(this.first, index, this.second))

    override fun add(element: ZKReference): Boolean {
        if (any { it.index == element.index }) error("Component visibility is already set for index ${element.index}")
        return super.add(element)
    }
}

/**
 * A list of input or references UTXOs that this ZKCommand needs to have access to.
 *
 * When building a [WireTransaction], this information is used to determine whether it
 * should be enforced that for each relevant [StateRef], its UTXO was private in the transaction that created it.
 * For each reference there are three options:
 * - private: the UTXO will NOT be allowed to be public in the transaction that created it.
 * - any: the UTXO WILL be allowed to be public in the transaction that created it.
 * - not mentioned at all: the UTXO WILL be allowed to be public in the transaction that created it.
 *
 * When building a [ZKVerifierTransaction], this information is used to determine which UTXOs
 * should be present in the witness. For each reference there are three options:
 * - private: the UTXO is: present in the witness
 * - any: the UTXO is present in the witness
 * - not mentioned at all: the UTXO is NOT present in the witness
 */
@ZKCommandMetadataDSL
class ZKProtectedComponentList : ArrayList<ZKProtectedComponent>() {

    fun public(type: KClass<out ContractState>): Pair<KClass<out ContractState>, Boolean> = type to false
    fun private(type: KClass<out ContractState>): Pair<KClass<out ContractState>, Boolean> = type to true

    infix fun Pair<KClass<out ContractState>, Boolean>.at(index: Int) = add(ZKProtectedComponent(this.first, index, this.second))

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
class ZKCommandMetadata(val commandKClass: KClass<out ZKCommandData>) {
    init {
        require(commandKClass.hasAnnotation<ZKP>()) { "Commands must be annotated with `@ZKP`" }
    }
    val scopedCommandName: String by lazy { commandKClass.scopedName ?: error("Command classes must be a named class") }

    /**
     * Information on the circuit and related artifacts to be used.
     *
     * If the command is marked private, but this is null, ZKFlow will
     * try to find the circuit based on some default rules. If that fails,
     * an error is thrown.
     */
    var circuit: ZKCircuit? = null

    var numberOfSigners = 0

    internal val inputs = ZKReferenceList()
    internal val references = ZKReferenceList()
    internal val outputs = ZKProtectedComponentList()

    /**
     * Flag that controls whether to make the command data of this command visible both publicly and privately.
     * `false` means only publicly, `true` means both publicly and privately.
     */
    var command = false

    /**
     * Flag that controls whether to make the notary group visible both publicly and privately.
     * `false` means only publicly, `true` means both publicly and privately.
     */
    var notary = false

    /**
     * Flag that controls whether to make the timeWindow group visible both publicly and privately.
     * `false` means only publicly, `true` means both publicly and privately.
     */
    var timeWindow = false

    /**
     * Flag that controls whether to make the parameters group visible both publicly and privately.
     * `false` means only publicly, `true` means both publicly and privately.
     */
    var networkParameters = false

    fun circuit(init: ZKCircuit.() -> Unit): ZKCircuit {
        circuit = ZKCircuit().apply(init)
        return circuit!!
    }

    fun inputs(init: ZKReferenceList.() -> Unit) = inputs.apply(init)
    fun references(init: ZKReferenceList.() -> Unit) = references.apply(init)
    fun outputs(init: ZKProtectedComponentList.() -> Unit) = outputs.apply(init)

    fun resolved(): ResolvedZKCommandMetadata = ResolvedZKCommandMetadata(
        circuit.resolve(this),
        commandKClass,
        numberOfSigners,
        inputs.toList(),
        references.toList(),
        outputs.toList(),
        command,
        notary,
        timeWindow,
        networkParameters,
    )
}

fun ZKCommandData.commandMetadata(init: ZKCommandMetadata.() -> Unit): ResolvedZKCommandMetadata {
    return commandMetadata(this::class, init)
}

fun commandMetadata(commandKClass: KClass<out ZKCommandData>, init: ZKCommandMetadata.() -> Unit): ResolvedZKCommandMetadata {
    return ZKCommandMetadata(commandKClass).apply(init).resolved()
}
