package com.ing.zknotary.common.zkp

import com.ing.zknotary.gradle.zinc.util.CircuitConfigurator
import net.corda.core.contracts.ComponentGroupEnum
import java.io.File

class CircuitMetaData(
    val name: String,
    val componentGroupSizes: Map<ComponentGroupEnum, Int>,
    val folder: File
) {
    companion object {
        const val CONFIG_CIRCUIT_FILE = "config.json"

        fun fromConfig(circuitFolder: File, commandPos: Int = 0): CircuitMetaData {
            val config = CircuitConfigurator(circuitFolder, CONFIG_CIRCUIT_FILE).circuitConfiguration

            return CircuitMetaData(
                config.groups.commandGroup.commands[commandPos].name,
                mapOf(ComponentGroupEnum.SIGNERS_GROUP to config.groups.signerGroup.signerListSize),
                circuitFolder
            )
        }
    }
}
