plugins {
    kotlin("jvm")
    id("idea")
    id("net.corda.plugins.cordapp")
    id("net.corda.plugins.quasar-utils")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization")
}

cordapp {
    val platformVersion: String by project
    targetPlatformVersion.set(platformVersion.toInt())
    minimumPlatformVersion.set(platformVersion.toInt())
    workflow {
        name.set("Zk Notary App")
        vendor.set("ING Bank NV")
        licence.set("Apache License, Version 2.0")
        versionId.set(1)
    }
}

dependencies {
    // These are required for our implementation and included in the JAR, we also expose their API for modules that depend on us.
    api(project(":utils"))
    api(project(":crypto"))
    api(project(":serialization"))

    // For Witness JSON serialization
    val kotlinxSerializationVersion: String by project
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // Corda dependencies.
    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    cordaCompile("$cordaReleaseGroup:corda-core:$cordaVersion")
    cordaRuntime("$cordaReleaseGroup:corda:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-node:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-jackson:$cordaVersion")

    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
    testImplementation(project(":test-utils"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
}

// TODO: We will have to enable explicitApi soon:
// https://kotlinlang.org/docs/reference/whatsnew14.html#explicit-api-mode-for-library-authors
// kotlin {
//     explicitApi = Strict
// }
// TODO: Introduce kotlin/binary-compatibility-validator: https://github.com/Kotlin/binary-compatibility-validator

// TODO: This should probably become a fat jar at some point, with its dependencies included
publishing {
    publications {
        create<MavenPublication>("zkFlow") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ingzkp/zkflow")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}