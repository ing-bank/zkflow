package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ZKNotaryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("zkp", ZKNotaryExtension::class.java)
        project.task("hello") {
            it.doLast {
                println("${extension.message} from ${extension.greeter}")
            }
        }
    }
}