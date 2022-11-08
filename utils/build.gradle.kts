plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
    jacoco
}

dependencies {
    val kotlinPoetVersion: String by project
    api("com.squareup:kotlinpoet:$kotlinPoetVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

publishing {
    publications {
        create<MavenPublication>("zkUtil") {
            from(components["java"])
        }
    }
}
