plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
}

kotlin {
    // explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
}

publishing {
    publications {
        create<MavenPublication>("zkUtil") {
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
