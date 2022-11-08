plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("maven-publish")
}

dependencies {
    implementation(project(":zinc-poet:zinc-code-generation"))
    compileOnly(kotlin("gradle-plugin-api"))

    // Loaded so we can apply the plugin on project we are applied on.
    val kotlinVersion: String by project
    runtimeOnly("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:$kotlinVersion")

    val kspVersion: String by project
    runtimeOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
}

tasks.compileKotlin {
    kotlinOptions.allWarningsAsErrors =
        false // Because we can't suppress the warning about multiple Kotlin versions, caused by the Gradle plugin API
}

gradlePlugin {
    plugins {
        create("zkFlowPlugin") {
            id = "com.ing.zkflow.gradle-plugin"
            implementationClass = "com.ing.zkflow.gradle.plugin.ZKFlowPlugin"
        }
    }
}
