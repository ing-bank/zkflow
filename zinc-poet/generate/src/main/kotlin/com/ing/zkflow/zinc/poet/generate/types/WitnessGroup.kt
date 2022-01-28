package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincType

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
}
