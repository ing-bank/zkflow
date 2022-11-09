buildscript {
    // Please note that this is only required because the ZKFlow artifacts are currently not published to a public maven repository.
    // For that reason, we always ensure that the latest version of ZKFlow is published locally before running the CorDapp build.
    println("ENSURING LATEST ZKFLOW IS PUBLISHED TO LOCAL MAVEN REPOSITORY...")
    exec {
        workingDir = projectDir.parentFile
        executable = "./gradlew"
        args("publishToMavenLocal")
    }

    extra.apply{
        set("corda_release_version", "4.8.5")
    }
}

plugins {
    kotlin("jvm") version "1.3.72"
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
        versionId(1)
    }
    workflow {
        name("Example workflows")
        vendor("Example Org")
        versionId(1)
    }
    signing {
        enabled(false)
    }
    sealing {
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
    val corda_release_version: String by project
    cordaCompile("net.corda:corda-core:$corda_release_version")
    cordaRuntime("net.corda:corda:$corda_release_version")
    cordaCompile("net.corda:corda-node:$corda_release_version")
    cordaCompile("net.corda:corda-jackson:$corda_release_version")

    testImplementation("net.corda:corda-node-driver:$corda_release_version")
    testImplementation("net.corda:corda-test-utils:$corda_release_version")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    testImplementation("com.ing.zkflow:test-utils:1.0-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("rpc")
    }
}

tasks.register<Test>("rpcTest") {
    useJUnitPlatform {
        includeTags("rpc")
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

tasks.register<net.corda.plugins.Cordform>("deployNodes") {
    dependsOn("jar")

    nodeDefaults{
        projectCordapp{
            deploy = true
            config(projectDir.resolve("config/app-config-rpc-test.conf"))
        }
        runSchemaMigration = true
    }
    node {
        name("O=ZKNotary,L=London,C=GB")
        notary = mapOf(Pair("validating", false))
        p2pPort(10001)
        rpcSettings {
            address("localhost:10002")
            adminAddress("localhost:10003")
        }

    }
    node {
        name("O=Issuer,L=Amsterdam,C=NL")
        p2pPort(10101)
        rpcSettings {
            address("localhost:10102")
            adminAddress("localhost:10103")
        }
        rpcUsers = listOf(mapOf( Pair("user", "user1"), Pair("password", "test"), Pair("permissions", listOf("ALL"))))
    }
}

// tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
//     doFirst {
//         println("ENSURING LATEST ZKFLOW IS PUBLISHED TO LOCAL MAVEN REPOSITORY...")
//         exec {
//             workingDir = projectDir.parentFile
//             executable = "./gradlew"
//             args("publishToMavenLocal")
//         }
//     }
// }