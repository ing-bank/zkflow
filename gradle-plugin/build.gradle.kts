plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("maven-publish")
}

dependencies {
    implementation(project(":notary"))

    val kotlinxSerializationBflVersion: String by project
    implementation("com.ing.serialization.bfl:kotlinx-serialization-bfl:$kotlinxSerializationBflVersion")

    // Loaded so we can apply the plugin on project we are applied on.
    val kotlinVersion: String by project
    runtimeOnly("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:$kotlinVersion")
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
