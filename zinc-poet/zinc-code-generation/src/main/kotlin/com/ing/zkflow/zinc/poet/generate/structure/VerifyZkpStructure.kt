package com.ing.zkflow.zinc.poet.generate.structure

import net.corda.core.internal.exists

fun main() {
    if (structureFile.exists()) {
        val savedStructure = readSavedStructure(structureFile)
        val newStructure = generateZkpStructure()
        verifyZkpStructure(savedStructure, newStructure)
    }
}
