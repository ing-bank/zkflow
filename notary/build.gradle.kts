import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("symbol-processing") version "1.4.10-dev-experimental-20201110"
    kotlin("jvm")
    id("idea")
    id("com.diffplug.gradle.spotless")
    id("net.corda.plugins.cordapp")
    id("net.corda.plugins.quasar-utils")
    id("io.gitlab.arturbosch.detekt")
}

cordapp {
    val platformVersion: String by project
    targetPlatformVersion = platformVersion.toInt()
    minimumPlatformVersion = platformVersion.toInt()
    workflow {
        name = "Zk Notary App"
        vendor = "ING Bank NV"
        licence = "Apache License, Version 2.0"
        versionId = 1
    }
}

// We need to tell Gradle where to find our generated code
val generatedSourcePath = "${buildDir.name}/generated/ksp/src/main/kotlin"
val testConfigResourcesDir = "$rootDir/config/test"
sourceSets {
    main {
        java {
            srcDir(file(generatedSourcePath))
        }
    }
    test {
        resources {
            srcDir(testConfigResourcesDir)
        }
    }
}
idea {
    module {
        // We also need to tell Intellij to mark it correctly as generated source
        generatedSourceDirs = generatedSourceDirs + file(generatedSourcePath)

        // Tell Intellij to download sources for dependencies
        isDownloadSources = true
    }
}

dependencies {
    val kotlinVersion: String by project
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // Testing
    val junit5Version: String by project
    val kotestVersion: String by project
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")

    // Corda dependencies.
    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    cordaCompile("$cordaReleaseGroup:corda-core:$cordaVersion")
    cordaRuntime("$cordaReleaseGroup:corda:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-node:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-jackson:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")

    // Annotation processing & Code generation
    // kapt(project(":generator"))
    compileOnly(project(":generator"))
    ksp(project(":generator"))
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("${buildDir.relativeTo(rootDir).path}/generated/**")
        val ktlintVersion: String by project
        ktlint(ktlintVersion)
    }
}

detekt {
    config = files("${rootDir}/config/detekt/detekt.yml")
}

task<Test>("slowTest") {
    val root = project.rootDir.absolutePath
    inputs.dir("$root/prover/circuits")

    useJUnitPlatform {
        includeTags("slow")

    }
    shouldRunAfter("test")
}

tasks.test {
    // We have a separate task for slow tests
    useJUnitPlatform {
        excludeTags("slow")
    }
}

// TODO: We will have to enable explicitApi soon:
// https://kotlinlang.org/docs/reference/whatsnew14.html#explicit-api-mode-for-library-authors
// kotlin {
//     explicitApi = Strict
// }

tasks.apply {
    matching { it is JavaCompile || it is KotlinCompile }.forEach { it.dependsOn(":checkJavaVersion") }

    withType<Detekt> {
        // Target version of the generated JVM bytecode. It is used for type resolution.
        jvmTarget = "1.8"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.3"
            apiVersion = "1.3"
            jvmTarget = "1.8"
            javaParameters = true   // Useful for reflection.
            freeCompilerArgs = listOf("-Xjvm-default=compatibility")
        }
    }

    withType<Jar> {
        // This makes the JAR's SHA-256 hash repeatable.
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    // This applies to all test types, both fast and slow
    withType<Test> {
        dependsOn("spotlessApply") // Autofix before check
        dependsOn("spotlessCheck") // Fail on remaining non-autofixable issues
        dependsOn("detekt")
        dependsOn(":prover:circuits") // Make sure that the Zinc circuit is ready to use when running tests

        val cores = Runtime.getRuntime().availableProcessors()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        logger.info("Using $cores cores to run $maxParallelForks test forks.")

        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            info {

            }
        }

        val mockZKP = System.getProperty("MockZKP")
        if (mockZKP != null) {
            systemProperty("MockZKP", true)
        }

        // Set the default log4j config file for tests
        systemProperty("log4j.configurationFile", "${project.buildDir}/resources/test/log4j2.xml")

        // Allow setting a custom log4j config file
        val logConfigPath = System.getProperty("log4j.configurationFile")
        if (logConfigPath != null) {
            systemProperty("log4j.configurationFile", logConfigPath)
        }

        // This file determines for the standard java.util.logging how and what is logged to the console
        // This is to configure logging that does not go through slf4j/log4j, like JUnit platform logging.
        systemProperty("java.util.logging.config.file", "${project.buildDir}/resources/test/logging-test.properties")
    }

}


