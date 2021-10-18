plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization") // TODO: Required only for CircuitConfigurator. Remove when that is removed.
}

dependencies {
    implementation(project(":crypto"))

    // We expose Kotlinx.serialization and BFL to our dependees (e.g. protocol module)
    val kotlinxSerializationVersion: String by project
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    val kotlinxSerializationBflVersion: String by project
    api("com.ing.serialization.bfl:kotlinx-serialization-bfl:$kotlinxSerializationBflVersion")

    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    compileOnly("$cordaReleaseGroup:corda-core:$cordaVersion")

    testImplementation(project(":test-utils"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
}

publishing {
    publications {
        create<MavenPublication>("zkSerialization") {
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
