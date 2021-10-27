plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("maven-publish")
}

dependencies {
    implementation(project(":compilation"))
    implementation(project(":obsolete")) // TODO: Required only for CircuitConfigurator. Remove when that is removed.

    // Loaded so we can apply the plugin on project we are applied on.
    val kotlinVersion: String by project
    runtimeOnly("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:$kotlinVersion")

    val kspVersion: String by project
    runtimeOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
}

gradlePlugin {
    plugins {
        create("zkFlowPlugin") {
            id = "com.ing.zkflow.gradle-plugin"
            implementationClass = "com.ing.zkflow.gradle.plugin.ZKFlowPlugin"
        }
    }
}

publishing {
    // TODO: remove this when using a real package repo
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
