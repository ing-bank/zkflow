plugins {
    id("com.ing.zknotary.gradle-plugin") version "0.1"
    kotlin("jvm")
    id("net.corda.plugins.cordapp")
    id("net.corda.plugins.quasar-utils")

    id("idea")
}

dependencies {
    implementation(kotlin("stdlib"))
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

    testImplementation("com.ing.zknotary:test-utils:0.1")
}

zkp {
    // generatorVersion = "0.1"
    // notaryVersion = "0.1"
}

cordapp {
    val platformVersion: String by project
    targetPlatformVersion = platformVersion.toInt()
    minimumPlatformVersion = platformVersion.toInt()
    workflow {
        name = "Test Cordapp"
        vendor = "ING Bank NV"
        licence = "Apache License, Version 2.0"
        versionId = 1
    }
}

tasks.apply {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.4"
            apiVersion = "1.4"
            jvmTarget = "1.8"
            javaParameters = true   // Useful for reflection.
            freeCompilerArgs = listOf("-Xjvm-default=compatibility")
        }
    }
}