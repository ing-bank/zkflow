package com.ing.zknotary.common.zkp

import com.ing.zknotary.gradle.zinc.util.CircuitConfigurator
import net.corda.core.contracts.ComponentGroupEnum
import java.io.File

class CircuitMetaData private constructor(
    val name: String,
    val componentGroupSizes: Map<ComponentGroupEnum, Int>,
    val folder: File
) {
    data class Builder(
        var name: String = "UNKNOWN",
        var componentGroupSizes: MutableMap<ComponentGroupEnum, Int> = mutableMapOf(),
        var folder: File = File("/tmp")
    ) {
        fun name(name: String) = apply { this.name = name }
        fun addComponentGroupSize(componentGroupEnum: ComponentGroupEnum, size: Int) =
            apply { this.componentGroupSizes[componentGroupEnum] = size }
        fun folder(folder: File) = apply { this.folder = folder }
        fun parseConfig(folder: File) = apply {
            val configPath = folder.resolve("config.json")
            require(configPath.exists()) {
                "Configuration file is expected at $configPath"
            }
            val config = CircuitConfigurator(configPath).circuitConfiguration

            this.name = config.command.name
            this.componentGroupSizes = mutableMapOf(ComponentGroupEnum.SIGNERS_GROUP to config.groups.signerGroup.signerListSize)
            this.folder = folder
        }

        fun build() = CircuitMetaData(name, componentGroupSizes, folder)
    }
}
