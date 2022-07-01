package com.ing.zkflow.zinc.poet.generate.structure

fun main() {
    val newStructure = generateZkpStructure()
    writeGeneratedStructure(structureFile, newStructure)
}
