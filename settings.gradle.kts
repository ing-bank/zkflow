@file:Suppress("UnstableApiUsage")
pluginManagement {
    // Preload versions of all plugins for all projects.
    // They should still be applied in the subproject's build.gradle.
    // Within subprojects, version should be omitted.
    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version kotlinVersion

        kotlin("plugin.serialization") version kotlinVersion apply false

        val spotlessPluginVersion: String by settings
        id("com.diffplug.spotless") version spotlessPluginVersion apply false

        val detektPluginVersion: String by settings
        id("io.gitlab.arturbosch.detekt") version detektPluginVersion apply false

        val cordaGradlePluginsVersion: String by settings
        id("net.corda.plugins.cordapp") version cordaGradlePluginsVersion apply false

        val licenseReportVersion: String by settings
        id("com.github.jk1.dependency-license-report") version licenseReportVersion apply false
    }

    repositories { // Only for these plugins. Repos for other dependencies should be defined in build.gradle.kts
        mavenLocal()
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://software.r3.com/artifactory/corda")
    }
}

include("utils")
include("crypto")
include("protocol")
include("gradle-plugin")
include("test-utils")
include("compiler-plugin-ksp")
include("serialization")
include("annotations")
include(":zinc-poet:zinc-poet")
include(":zinc-poet:zinc-bfl")
include(":zinc-poet:zinc-code-generation")
include(":integration-tests:fixtures")
include(":integration-tests:contracts")
include(":integration-tests:flows")
include(":integration-tests:serialization")
