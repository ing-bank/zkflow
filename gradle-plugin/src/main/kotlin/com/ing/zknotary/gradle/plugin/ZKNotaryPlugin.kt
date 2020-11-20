package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ZKNotaryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("zkp", ZKNotaryExtension::class.java)

        project.plugins.apply("symbol-processing")

        project.dependencies.add("implementation", "com.ing.zknotary:notary:${extension.notaryVersion}")
        project.dependencies.add("implementation", "com.ing.zknotary:generator:${extension.generatorVersion}")
        project.dependencies.add("ksp", "com.ing.zknotary:generator:${extension.generatorVersion}")
    }
}