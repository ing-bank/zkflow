plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("maven-publish")
    jacoco
}

// jacoco {
//     toolVersion = "0.8.7"
// }
//
// repositories {
//     maven("https://software.r3.com/artifactory/corda")
// }

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation(project(":test-utils"))

    implementation(project(":annotations"))

    val kotlinxSerializationVersion: String by project
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    val cordaVersion: String by project
    implementation("net.corda:corda-core:$cordaVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
        freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    }
}

publishing {
    publications {
        create<MavenPublication>("zkSerializationCandidate") {
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
