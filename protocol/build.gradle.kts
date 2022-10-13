plugins {
    kotlin("jvm")
    id("idea")
    id("net.corda.plugins.cordapp")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization")
    jacoco
}

cordapp {
    val platformVersion: String by project
    targetPlatformVersion(platformVersion.toInt())
    minimumPlatformVersion(platformVersion.toInt())
    workflow {
        name("ZKFlow Protocol Flows and Services")
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
    api(project(":annotations"))

    // For Witness JSON serialization
    val kotlinxSerializationVersion: String by project
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // For zinc name
    implementation(project(":zinc-poet:zinc-bfl"))

    // To be able to generate correct names for serializers. -->
    // TODO very likely it can be avoided. Used to speed up the process.
    //   Required in ZincTypeNames.kt:53 for KClass<*>.asClassName()
    val kotlinPoetVersion: String by project
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    // <--

    // Corda dependencies.
    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    cordaCompile("$cordaReleaseGroup:corda-core:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-node:$cordaVersion")
    cordaRuntime("$cordaReleaseGroup:corda:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-test-utils:$cordaVersion")

    val log4jVersion: String by project
    cordaCompile("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
    testImplementation(project(":test-utils"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.InternalSerializationApi"
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
