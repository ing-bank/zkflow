group = "com.ing.zknotary"
version = "0.1"

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("maven-publish")
    id("symbol-processing") version "1.4.10-dev-experimental-20201118"
}

dependencies {
    api("com.ing.zknotary:notary:0.1")
    api("com.ing.zknotary:generator:0.1")

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
    publications {
        create<MavenPublication>("zkNotary") {
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
