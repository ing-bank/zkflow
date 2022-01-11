plugins {
    kotlin("jvm")
    id("java-library")
    kotlin("plugin.serialization")
    id("maven-publish")
    jacoco
}

dependencies {
    implementation(project(":zinc-poet:bfl"))
    implementation(project(":zinc-poet:poet"))
    implementation(project(":protocol"))
    implementation(project(":serialization")) // TODO remove when serialization-candidate is mainline or code is generated completely from serial descriptors
    implementation(project(":serialization-candidate"))
    implementation(project(":annotations"))

    val kotlinxSerializationVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    kotlinCompilerPluginClasspath(project(":utils"))
    kotlinCompilerPluginClasspath(project(":annotations"))
    kotlinCompilerPluginClasspath(project(":serialization-candidate"))
    kotlinCompilerPluginClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$kotlinxSerializationVersion")

    val arrowMetaVersion: String by project
    kotlinCompilerPluginClasspath("io.arrow-kt:arrow-meta:$arrowMetaVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn += "clean"
    dependsOn += ":compiler-plugin-arrow:jar"
    kotlinOptions {
        useIR = true
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xplugin=$rootDir/compiler-plugin-arrow/build/libs/compiler-plugin-arrow-$version.jar"
        freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
        freeCompilerArgs += "-Xopt-in=kotlinx.serialization.InternalSerializationApi"
        freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    }
}

publishing {
    publications {
        create<MavenPublication>("zkZincPoetGenerate") {
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
