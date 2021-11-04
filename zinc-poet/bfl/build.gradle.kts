plugins {
    kotlin("jvm")
    `java-library`
    jacoco
    id("io.gitlab.arturbosch.detekt")
    id("com.diffplug.spotless")
    id("maven-publish")
    id("org.owasp.dependencycheck")
}

dependencies {
    implementation(project(":zinc-poet:poet"))

    val kotlinxSerializationVersion: String by project
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}

java {
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
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
