plugins {
    kotlin("jvm")
    id("java-library")
    id("maven-publish")
    jacoco
}

dependencies {
    implementation(project(":utils"))
    api(project(":zinc-poet:zinc-poet"))

    val kotlinxSerializationVersion: String by project
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    testImplementation(project(":test-utils"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.io.path.ExperimentalPathApi"
}

publishing {
    publications {
        create<MavenPublication>("zkZincPoetBfl") {
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
