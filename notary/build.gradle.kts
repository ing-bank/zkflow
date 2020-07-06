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
    val junitVersion: String by project
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.mockito:mockito-core:2.+")

    // Corda dependencies.
    val cordaReleaseGroup : String by project
    val cordaVersion : String by project
    cordaCompile("$cordaReleaseGroup:corda-core:$cordaVersion")
    cordaRuntime("$cordaReleaseGroup:corda:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-node:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-jackson:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")

    // Other
    implementation(group = "net.java.dev.jna", name = "jna", version = "5.3.1")
}

spotless {
    kotlin {
        ktlint("0.37.1")
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

    withType<Test> {
        dependsOn("spotlessCheck") // Make sure we fail early on style

        val cores = Runtime.getRuntime().availableProcessors()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        logger.info("Using $cores cores to run $maxParallelForks test forks.")

        // Here you can specify any env vars for tests, for instance the path to the prover lib
        // environment "LD_LIBRARY_PATH", "~/pepper_deps/lib/"
    }
}

