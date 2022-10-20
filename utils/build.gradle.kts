plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
    jacoco
}

dependencies {
    val kotlinPoetVersion: String by project
    api("com.squareup:kotlinpoet:$kotlinPoetVersion")
}
kotlin {
    // explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
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
