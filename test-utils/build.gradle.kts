import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
}

dependencies {
    implementation(project(":notary"))

    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    implementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
    implementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
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
