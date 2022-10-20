plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("maven-publish")
    jacoco
}

dependencies {
    implementation(project(":annotations"))
    implementation(project(":crypto"))
    implementation(project(":utils"))
    implementation(project(":zinc-poet:zinc-poet"))

    val kotlinxSerializationVersion: String by project
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    val cordaVersion: String by project
    val cordaReleaseGroup: String by project
    compileOnly("$cordaReleaseGroup:corda-core:$cordaVersion")

    testImplementation(kotlin("test-junit5"))
    testImplementation(project(":test-utils"))
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-core:$cordaVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
        freeCompilerArgs += "-Xopt-in=kotlinx.serialization.InternalSerializationApi"
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
