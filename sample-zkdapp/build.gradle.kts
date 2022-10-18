buildscript {
    extra.apply{
        set("corda_release_version", "4.8.5")
    }
}

plugins {
    kotlin("jvm") version "1.5.31"
    id("com.ing.zkflow.gradle-plugin") version "1.0-SNAPSHOT"

    id("net.corda.plugins.quasar-utils")  version "5.0.12"
    id("net.corda.plugins.cordapp") version "5.0.12"
    id("net.corda.plugins.cordformation")  version "5.0.15"
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
    signing {
        enabled(false)
    }
}

repositories {
    mavenLocal()
    google()
    maven("https://jitpack.io")
    maven("https://software.r3.com/artifactory/corda")
    maven("https://repo.gradle.org/gradle/libs-releases")
    mavenCentral()
}

dependencies {
    // TODO: Find a way to set this correclty from the ZKFlowPlugin. Already doing it there for 'implementation', but apparently
    // that is not enough?
    compile("com.ing.zkflow:protocol:1.0-SNAPSHOT")

    val kotlinVersion = "1.5.31"
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // TODO: Find a way to set this from the ZKFlowPlugin with correct version
    val kotlinxSerializationVersion = "1.3.1"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    kotlinCompilerPluginClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

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

// val publishZKFlow = tasks.register("publishZKFlow") {
//     this.doFirst {
//         exec {
//             workingDir = projectDir.resolve("..")
//             executable = "./gradlew"
//             args = listOf("publishToMavenLocal", "--info")
//         }
//     }
// }
//
// tasks.compileKotlin {
//     dependsOn(publishZKFlow)
// }
//
// tasks.compileTestKotlin {
//     dependsOn(publishZKFlow)
// }


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

tasks.jar {
    // This makes the JAR's SHA-256 hash repeatable.
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    exclude("**/module-info.class")
    exclude("README.txt")
    exclude("LICENSE")
    exclude("LICENSE-junit.txt")
    exclude("log4j2-test.xml")
}

// apply(plugin = "net.corda.plugins.cordapp")
// apply(plugin = "net.corda.plugins.cordformation")
// apply(plugin = "net.corda.plugins.quasar-utils")
//
//Task to deploy the nodes in order to bootstrap a network
tasks.register<net.corda.plugins.Cordform>("deployNodes") {
    dependsOn("jar")

    /* This property will load the CorDapps to each of the node by default, including the Notary. You can find them
     * in the cordapps folder of the node at build/nodes/Notary/cordapps. However, the notary doesn't really understand
     * the notion of cordapps. In production, Notary does not need cordapps as well. This is just a short cut to load
     * the Corda network bootstrapper.
     */
    nodeDefaults{
        projectCordapp{
            deploy = true
            config(project.file("app-config-mock.conf"))
        }
        runSchemaMigration = true //This configuration is for any CorDapps with custom schema, We will leave this as true to avoid
        //problems for developers who are not familiar with Corda. If you are not using custom schemas, you can change
        //it to false for quicker project compiling time.
    }
    node {
        name("O=Zknotary,L=London,C=GB")
        notary = mapOf(Pair("validating", false))
        p2pPort(10001)
        rpcSettings {
            address("localhost:10002")
            adminAddress("localhost:10003")
        }
    }
    node {
        name("O=Zk1,L=London,C=GB")
        p2pPort(10101)
        rpcSettings {
            address("localhost:10102")
            adminAddress("localhost:10103")
        }
        rpcUsers = listOf(mapOf( Pair("user", "user1"), Pair("password", "test"), Pair("permissions", listOf("ALL"))))
    }
//    node {
//        name("O=Zk2,L=New York,C=US")
//        p2pPort(10201)
//        rpcSettings {
//            address("localhost:10202")
//            adminAddress("localhost:10203")
//        }
//        rpcUsers = listOf(mapOf( Pair("user", "user1"), Pair("password", "test"), Pair("permissions", listOf("ALL"))))
//    }
//    node {
//        name("O=Zk3,L=New York,C=US")
//        p2pPort(10301)
//        rpcSettings {
//            address("localhost:10302")
//            adminAddress("localhost:10303")
//        }
//        rpcUsers = listOf(mapOf( Pair("user", "user1"), Pair("password", "test"), Pair("permissions", listOf("ALL"))))
//    }
}