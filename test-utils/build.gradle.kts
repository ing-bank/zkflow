import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization") // Required for serializable fixtures
}

dependencies {
    // We explicitly don't provide the protocol as part of our JAR: users of test-utils should have the protocol on their class path already
    compileOnly(project(":protocol"))

    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    api("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
    api("$cordaReleaseGroup:corda-node-driver:$cordaVersion")

    implementation(project(":serialization"))

    implementation(project(":zinc-poet:zinc-code-generation"))

    val junit5Version: String by project
    implementation("org.junit.jupiter:junit-jupiter-params:$junit5Version")

    val kotestVersion: String by project
    implementation("io.kotest:kotest-assertions-core:$kotestVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
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
