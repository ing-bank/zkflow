plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("maven-publish")
    jacoco
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation(project(":test-utils"))

    implementation(project(":annotations"))
    implementation(project(":crypto"))
    implementation(project(":utils"))

    val kotlinxSerializationVersion: String by project
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    val cordaVersion: String by project
    compileOnly("net.corda:corda-test-utils:$cordaVersion")
    compileOnly("net.corda:corda-core:$cordaVersion")
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
