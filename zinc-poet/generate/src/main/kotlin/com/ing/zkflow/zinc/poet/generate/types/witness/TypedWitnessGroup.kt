package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.zinc.poet.generate.types.WitnessGroup

internal data class TypedWitnessGroup(
    override val groupName: String,
    val module: BflType,
    private val groupSize: Int,
) : WitnessGroup {
    override val isPresent: Boolean = groupSize > 0
    override val options: List<WitnessGroupOptions> = emptyList()
    override val dependencies: List<BflType> = listOf(module)
    override val serializedType: ZincType = module.toZincId()
    override val generateHashesMethod: ZincFunction? = null
}
