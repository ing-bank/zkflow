package com.ing.zknotary.common.zkp

import com.ing.zknotary.gradle.zinc.util.CircuitConfigurator
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.utilities.seconds
import java.io.File
import java.time.Duration

data class CircuitMetaData(
    val name: String,
    val componentGroupSizes: Map<ComponentGroupEnum, Int>,
    val javaClass2ZincType: Map<String, String>,
    val buildFolder: File,

    val buildTimeout: Duration,
    val setupTimeout: Duration,
    val provingTimeout: Duration,
    val verificationTimeout: Duration
) {
    companion object {
        const val CONFIG_CIRCUIT_FILE = "config.json"

        fun fromConfig(circuitFolder: File, commandPos: Int = 0): CircuitMetaData {
            val config = CircuitConfigurator(circuitFolder, CONFIG_CIRCUIT_FILE).circuitConfiguration

            val javaClass2ZincType = config.circuit.states.associate {
                it.javaClass to it.zincType
            }

            return CircuitMetaData(
                config.groups.commandGroup.commands[commandPos].name,
                mapOf(ComponentGroupEnum.SIGNERS_GROUP to config.groups.signerGroup.signerListSize),
                javaClass2ZincType,
                circuitFolder,
                config.circuit.buildTimeout.seconds,
                config.circuit.setupTimeout.seconds,
                config.circuit.provingTimeout.seconds,
                config.circuit.verificationTimeout.seconds
            )
        }
    }
}
