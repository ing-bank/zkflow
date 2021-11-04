package com.ing.zinc.bfl.generator

import com.ing.zinc.poet.ZincFile

data class CodeGenerationOptions(
    val witnessGroupOptions: List<WitnessGroupOptions>
) {
    fun addConstants(zincFileBuilder: ZincFile.Builder) {
        witnessGroupOptions.forEach {
            zincFileBuilder.add(it.witnessSizeConstant)
        }
    }
}
