import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":notary"))

    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    api("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
    api("$cordaReleaseGroup:corda-node-driver:$cordaVersion")

    val kotlinxSerializationVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    val kotlinxSerializationBflVersion: String by project
    implementation("com.ing.serialization.bfl:kotlinx-serialization-bfl:$kotlinxSerializationBflVersion")

    val kotestVersion: String by project
    implementation("io.kotest:kotest-assertions-core:$kotestVersion")
}

kotlin {
    explicitApi = Strict
}

publishing {
    publications {
        create<MavenPublication>("zkTestUtils") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ingzkp/zk-notary")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
