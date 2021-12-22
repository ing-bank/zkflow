plugins {
    kotlin("jvm")
    id("idea")
    id("net.corda.plugins.cordapp")
    id("net.corda.plugins.quasar-utils")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization")
    jacoco
}
jacoco {
    toolVersion = "0.8.7"
}

cordapp {
    val platformVersion: String by project
    targetPlatformVersion(platformVersion.toInt())
    minimumPlatformVersion(platformVersion.toInt())
    workflow {
        name("ZKFlow Protocol App")
        vendor("ING Bank NV")
        licence("Apache License, Version 2.0")
        versionId(1)
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
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
}

val jacocoExcludes = listOf(
    "com.ing.zkflow.client.flows.*"
)

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "CLASS"
            excludes = jacocoExcludes
            limit {
                counter = "LINE"
                minimum = "0.6".toBigDecimal()
            }
        }
    }
}

project.afterEvaluate {
    tasks.jacocoTestReport {
        sourceDirectories.setFrom(
            files(
                sourceDirectories.files.map {
                    fileTree(it) {
                        exclude("**/com/ing/zkflow/client/flows/**")
                    }
                }
            )
        )
        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude("**/com/ing/zkflow/client/flows/**")
                    }
                }
            )
        )
        additionalClassDirs.setFrom(
            files(
                additionalClassDirs.files.map {
                    fileTree(it) {
                        exclude("**/com/ing/zkflow/client/flows/**")
                    }
                }
            )
        )
    }
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
