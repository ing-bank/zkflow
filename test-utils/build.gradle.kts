import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":protocol"))

    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    api("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
    api("$cordaReleaseGroup:corda-node-driver:$cordaVersion")

    val kotlinxSerializationVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    val kotlinxSerializationBflVersion: String by project
    implementation("com.ing.serialization.bfl:kotlinx-serialization-bfl:$kotlinxSerializationBflVersion")

    val kotestVersion: String by project
    implementation("io.kotest:kotest-assertions-core:$kotestVersion")

    val junit5Version: String by project
    implementation("org.junit.jupiter:junit-jupiter-params:$junit5Version")
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
            url = uri("https://maven.pkg.github.com/ingzkp/zkflow")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
