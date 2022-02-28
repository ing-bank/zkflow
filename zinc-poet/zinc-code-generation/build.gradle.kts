plugins {
    kotlin("jvm")
    id("java-library")
    kotlin("plugin.serialization")
    id("maven-publish")
    jacoco
}

dependencies {
    implementation(project(":utils"))
    api(project(":zinc-poet:zinc-bfl"))
    implementation(project(":protocol"))

    val kotlinxSerializationVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    val cordaVersion: String by project
    kotlinCompilerPluginClasspath("net.corda:corda-core:$cordaVersion")
    kotlinCompilerPluginClasspath(project(":utils"))
    kotlinCompilerPluginClasspath(project(":annotations"))
    kotlinCompilerPluginClasspath(project(":serialization-candidate"))

    val arrowMetaVersion: String by project
    kotlinCompilerPluginClasspath("io.arrow-kt:arrow-meta:$arrowMetaVersion")

    testImplementation(project(":test-utils"))
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
