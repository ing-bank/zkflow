import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.diffplug.gradle.spotless")
    id("net.corda.plugins.cordapp")
    id("net.corda.plugins.quasar-utils")
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

sourceSets {
    main {
        resources {
            srcDir(rootProject.file("config/dev"))
        }
    }
    test {
        resources {
            srcDir(rootProject.file("config/test"))
        }
    }
}

dependencies {
    val kotlinVersion: String by project
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
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
}

spotless {
    kotlin {
        val ktlintVersion: String by project
        ktlint(ktlintVersion)
    }
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

tasks.apply {
    matching { it is JavaCompile || it is KotlinCompile }.forEach { it.dependsOn(":checkJavaVersion") }

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

    // This applies to all test types, both fast and slow
    withType<Test> {
        dependsOn("spotlessCheck") // Make sure we fail early on style
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

        // Allow setting a custom log4j config file
        val logConfigPath = System.getProperty("log4j.configurationFile")
        if (logConfigPath != null) {
            systemProperty("log4j.configurationFile", logConfigPath)
        }

        // This file determines for the standard java.util.logging how and what is logged to the console
        systemProperty("java.util.logging.config.file", "${project.buildDir}/resources/test/logging-test.properties")
    }

}


