plugins {
    kotlin("jvm")
    id("idea")
    id("java-library")
    id("maven-publish")
    jacoco
}

dependencies {
    implementation(project(":protocol"))
    implementation(project(":zinc-poet:zinc-code-generation"))
    implementation(kotlin("stdlib"))

    val kspVersion: String by project
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    val kspTestingVersion: String by project
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:$kspTestingVersion")

    val kotlinPoetVersion: String by project
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    implementation("com.squareup:kotlinpoet-ksp:$kotlinPoetVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xopt-in=com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview"
    }
}

publishing {
    publications {
        create<MavenPublication>("kspCompilerPlugin") {
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
