pluginManagement {
    // Preload versions of all plugins for all projects.
    // They should still be applied in the subproject's build.gradle.
    // Within subprojects, version should be omitted.
    plugins {
        val kotlinVersion: String by settings
        kotlin("jvm") version kotlinVersion

        val spotlessPluginVersion: String by settings
        id("com.diffplug.gradle.spotless") version spotlessPluginVersion apply false

        val cordaGradlePluginsVersion: String by settings
        id("net.corda.plugins.cordapp") version cordaGradlePluginsVersion apply false
        id("net.corda.plugins.quasar-utils") version cordaGradlePluginsVersion apply false

        id("com.cosminpolifronie.gradle.plantuml") version "1.6.0" apply false
        id("org.danilopianini.gradle-latex") version "0.2.5" apply false
    }

    repositories { // Only for these plugins. Repos for other dependencies should be defined in build.gradle.kts
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://software.r3.com/artifactory/corda")
    }
}

include("notary")
include("docs:whitepaper")
include("prover")
