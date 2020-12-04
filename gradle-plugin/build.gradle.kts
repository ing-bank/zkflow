plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("maven-publish")
}

dependencies {
    val kspVersion: String by project
    api("com.google.devtools.ksp:symbol-processing:$kspVersion")
}

gradlePlugin {
    plugins {
        create("zkNotaryPlugin") {
            id = "com.ing.zknotary.gradle-plugin"
            implementationClass = "com.ing.zknotary.gradle.plugin.ZKNotaryPlugin"
        }
    }
}

publishing {
    // TODO: remove this when using a real package repo
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
