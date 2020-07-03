import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    apply(from = "${rootProject.projectDir}/config.gradle.kts")
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven("https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases")
        maven("https://software.r3.com/artifactory/corda")
        maven("https://repo.gradle.org/gradle/libs-releases")
        maven("https://plugins.gradle.org/m2/")
    }
}

// Preload versions of all plugins for all projects. They should still be loaded in the subprojects build.gradle.
// Within subprojects, version can be ommitted.
plugins {
    kotlin("jvm") version project.rootProject.extra["kotlinVersion"] as String
    id("com.diffplug.gradle.spotless") version project.extra["spotlessPluginVersion"] as String
    id("net.corda.plugins.cordapp") version project.extra["gradlePluginsVersion"] as String
    id("net.corda.plugins.quasar-utils") version project.extra["gradlePluginsVersion"] as String
}

allprojects {
    apply(from = "${rootProject.projectDir}/repositories.gradle")

    tasks.apply {
        withType<KotlinCompile> {
            kotlinOptions {
                languageVersion = "1.3"
                apiVersion = "1.3"
                jvmTarget = "1.8"
                javaParameters = true   // Useful for reflection.
            }
        }

        withType<Jar> {
            // This makes the JAR's SHA-256 hash repeatable.
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
        }

        withType<Test> {
            dependsOn("spotlessCheck") // Make sure we fail early on style

            val cores = Runtime.getRuntime().availableProcessors()
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
            logger.info("Using $cores cores to run $maxParallelForks test forks.")

            // Here you can specify any env vars for tests, for instance the path to the prover lib
            // environment "LD_LIBRARY_PATH", "~/pepper_deps/lib/"
        }
    }
}


