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
        fun fromConfig(folder: File, commandPos: Int = 0): CircuitMetaData {
            val configPath = folder.resolve("config.json")
            require(configPath.exists()) {
                "Configuration file is expected at $configPath"
            }
            val config = CircuitConfigurator(configPath).circuitConfiguration

            return CircuitMetaData(
                config.groups.commandGroup.commands[commandPos].name,
                mapOf(ComponentGroupEnum.SIGNERS_GROUP to config.groups.signerGroup.signerListSize),
                folder
            )
        }
    }
}
