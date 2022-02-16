plugins {
    kotlin("jvm") version "1.5.31"
    id("com.ing.zkflow.gradle-plugin")
    id("net.corda.plugins.cordapp") version "5.0.12"
}

cordapp {
    targetPlatformVersion(10)
    minimumPlatformVersion(10)
    contract {
        name("Example contracts")
        vendor("Example Org")
        licence("Apache license, version 2.0")
        versionId(1)
    }
    workflow {
        name("Example workflows")
        vendor("Example Org")
        licence("Apache license, version 2.0")
        versionId(1)
    }
}

repositories {
    google()
    maven("https://jitpack.io")
    maven("https://software.r3.com/artifactory/corda")
    maven("https://repo.gradle.org/gradle/libs-releases")
    mavenCentral()
}

dependencies {
    val kotlinVersion = "1.5.31"
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    val cordaReleaseGroup = "net.corda"
    val cordaVersion = "4.8.5"
    cordaCompile("$cordaReleaseGroup:corda-core:$cordaVersion")
    cordaRuntime("$cordaReleaseGroup:corda:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-node:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-jackson:$cordaVersion")

    kotlinCompilerPluginClasspath("net.corda:corda-core:$cordaVersion")

    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    testImplementation("com.ing.zkflow:test-utils:1.0-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
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
    systemProperty(
        "java.util.logging.config.file",
        "${project.buildDir}/resources/test/logging-test.properties"
    )
}

val testConfigResourcesDir = "${rootProject.rootDir}/config/test"
sourceSets {
    test {
        resources {
            srcDir(testConfigResourcesDir)
        }
    }
}
