plugins {
    kotlin("jvm")
    id("java-library")
    kotlin("plugin.serialization")
    id("maven-publish")
    jacoco
    id("com.google.devtools.ksp") version "1.5.31-1.0.0"
}

dependencies {
    implementation(project(":utils"))
    api(project(":zinc-poet:zinc-bfl"))
    implementation(project(":common"))
    implementation(project(":serialization"))
    implementation(project(":annotations"))

    val kotlinxSerializationVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    val cordaVersion: String by project
    compileOnly("net.corda:corda-core:$cordaVersion")
    testImplementation("net.corda:corda-core:$cordaVersion")

    kotlinCompilerPluginClasspath("net.corda:corda-core:$cordaVersion")
    kotlinCompilerPluginClasspath(project(":utils"))
    kotlinCompilerPluginClasspath(project(":annotations"))
    kotlinCompilerPluginClasspath(project(":serialization"))

    implementation(project(":compiler-plugin-ksp"))
    ksp(project(":compiler-plugin-ksp"))

    testImplementation(project(":test-utils"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn += "clean"
    kotlinOptions {
        useIR = true
        jvmTarget = "1.8"
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
