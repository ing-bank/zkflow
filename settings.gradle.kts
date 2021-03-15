@file:Suppress("UnstableApiUsage")
pluginManagement {
    // Preload versions of all plugins for all projects.
    // They should still be applied in the subproject's build.gradle.
    // Within subprojects, version should be omitted.
    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version kotlinVersion

        val spotlessPluginVersion: String by settings
        id("com.diffplug.gradle.spotless") version spotlessPluginVersion apply false

        val detektPluginVersion: String by settings
        id("io.gitlab.arturbosch.detekt") version detektPluginVersion apply false

        val cordaGradlePluginsVersion: String by settings
        id("net.corda.plugins.cordapp") version cordaGradlePluginsVersion apply false
        id("net.corda.plugins.quasar-utils") version cordaGradlePluginsVersion apply false

        val plantUmlVersion: String by settings
        id("com.cosminpolifronie.gradle.plantuml") version plantUmlVersion apply false
    }

    repositories { // Only for these plugins. Repos for other dependencies should be defined in build.gradle.kts
        mavenLocal()
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://software.r3.com/artifactory/corda")
    }
}

include("notary")
include("zinc-platform-sources")
include("gradle-plugin")
include("test-utils")
include("docs")
