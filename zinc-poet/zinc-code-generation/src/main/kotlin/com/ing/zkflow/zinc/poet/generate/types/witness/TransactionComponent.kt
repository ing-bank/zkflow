package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.COMMANDS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUT_STATEREFS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.NOTARY
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PARAMETERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_INPUT_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_REFERENCE_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.TIME_WINDOW

/**
 * This interface abstracts some common properties that are used to generate [Witness].
 */
interface TransactionComponent {
    /**
     * Name to use as variable in the [Witness].
     */
    val groupName: String

    /**
     * The [ZincType] to use in the variable in the [Witness].
     */
    val serializedType: ZincType

    /**
     * Whether this group should be present in the [Witness].
     * Groups that are not present are filtered out completely.
     */
    val isPresent: Boolean

    /**
     * The [TransactionComponentOptions] for this group.
     */
    val options: List<TransactionComponentOptions>

    /**
     * Dependencies that should be imported into [Witness].
     */
    val dependencies: List<BflType>

    /**
     * Implementation for the generate_leaf_hashes method.
     */
    val generateHashesMethod: ZincMethod?

    val publicInputFieldName: String get() = groupName.toPublicInputFieldName()
}

fun String.toPublicInputFieldName(): String = publicInputFieldNameMap[this]
    ?: error("No publicInputFieldNameMap mapping defined for `$this`")

private val publicInputFieldNameMap: Map<String, String> = mapOf(
    INPUTS to "input_hashes",
    OUTPUTS to "output_hashes",
    REFERENCES to "reference_hashes",
    COMMANDS to "command_components",
    NOTARY to "notary_components",
    PARAMETERS to "parameters_components",
    TIME_WINDOW to "time_window_components",
    SIGNERS to "signers_components",
    SERIALIZED_INPUT_UTXOS to "input_utxo_hashes",
    SERIALIZED_REFERENCE_UTXOS to "reference_utxo_hashes",
    INPUT_STATEREFS to "input_stateref_components"
)
