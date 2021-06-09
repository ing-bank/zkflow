package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.zinc.util.CircuitConfigurator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
This task receives the input commandName as command line argument and creates a zinc directory under src/main with the
provided command name. It also copies sample code for consts, contract rules, and contract state that should be manually
implemented on zkdapp side.
 **/
open class CreateCircuitFromConfigurationTask : DefaultTask() {
    private var configFileName = "configFile"
    @Option(option = "configFile", description = "Set the file name for circuit configuration.")
    fun setConfiguration(configFile: String) {
        configFileName = configFile
    }

    @Input
    fun getConfiguration(): String {
        return configFileName
    }

    @TaskAction
    fun createCircuitFromConfiguration() {
        val configurator = CircuitConfigurator(project.rootDir.resolve(configFileName).absolutePath)
        configurator.testConfig()
    }
}
