plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    val cordaVersion: String by project
    compileOnly("net.corda:corda-core:$cordaVersion")
}

publishing {
    publications {
        create<MavenPublication>("zkAnnotations") {
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
