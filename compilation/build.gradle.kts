plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
}

dependencies {
    implementation(project(":protocol"))
    implementation(project(":obsolete")) // TODO: Required only for CircuitConfigurator. Remove when that is removed.

    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    compileOnly("$cordaReleaseGroup:corda-core:$cordaVersion")
}

publishing {
    publications {
        create<MavenPublication>("zkCompilation") {
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
