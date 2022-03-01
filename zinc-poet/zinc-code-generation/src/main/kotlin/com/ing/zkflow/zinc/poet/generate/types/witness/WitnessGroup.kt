package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.COMMANDS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUTS
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
interface WitnessGroup {
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
     * The [WitnessGroupOptions] for this group.
     */
    val options: List<WitnessGroupOptions>

    /**
     * Dependencies that should be imported into [Witness].
     */
    val dependencies: List<BflType>

    /**
     * Implementation for the generate_leaf_hashes method.
     */
    val generateHashesMethod: ZincFunction?

    val publicInputFieldName: String get() = groupName.toPublicInputFieldName()
}

fun String.toPublicInputFieldName(): String = publicInputFieldNameMap[this]
    ?: error("No publicInputFieldNameMap mapping defined for `$this`")

private val publicInputFieldNameMap: Map<String, String> = mapOf(
    INPUTS to "input_hashes",
    OUTPUTS to "output_hashes",
    REFERENCES to "reference_hashes",
    COMMANDS to "command_hashes",
    NOTARY to "notary_hashes",
    PARAMETERS to "parameters_hashes",
    TIME_WINDOW to "time_window_hashes",
    SIGNERS to "signers_hashes",
    SERIALIZED_INPUT_UTXOS to "input_utxo_hashes",
    SERIALIZED_REFERENCE_UTXOS to "reference_utxo_hashes",
)
