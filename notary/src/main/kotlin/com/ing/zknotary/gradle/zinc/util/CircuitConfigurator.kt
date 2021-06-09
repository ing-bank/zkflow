package com.ing.zknotary.gradle.zinc.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class CircuitConfigurator(private val configFilePath: String) {

    @Serializable
    data class Command(val command: String)

    fun readConfigFile() {
        val configFile = File(configFilePath)
        // println(configFile.readText())

        // val command = Json.decodeFromString<Command>(configFile.readText())
        // val instance = Command("move")
        // val stringOutput = Json.encodeToJsonElement(instance)

        // println(stringOutput)
        // val command = Json.decodeFromString<Command>(configFile.readText())

        // println(command.command)
    }

    fun testConfig() {
        println("before format")
        val format = Json { ignoreUnknownKeys = true }
        println("before ser")
        val commandSerialized = format.encodeToString(Command.serializer(), Command("create"))
        println("after ser")
        println(commandSerialized)

        println("before deser")
        val commandDeser = format.decodeFromString(Command.serializer(), commandSerialized)
        println("after deser")

    }
}
