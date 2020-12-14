plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("maven-publish")
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    val autoServiceVersion: String by project
    val kotlinPoetVersion: String by project
    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
    implementation("com.google.auto.service:auto-service:$autoServiceVersion")
    kapt("com.google.auto.service:auto-service:$autoServiceVersion")

    val kspVersion: String by project
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    testImplementation("com.google.devtools.ksp:symbol-processing:$kspVersion")

    val kspTestingVersion: String by project
    val kotlinVersion: String by project
    implementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:$kspTestingVersion")
    // Require to override everything included by kotlin-compile-testing
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")

    // This is necessary, because the version included by kotlin-compile-testing (4.8.86)
    // is breaking the Corda classloading.
    // TODO: This is dangerous of course, so we need to keep an eye on this
    implementation("io.github.classgraph:classgraph:4.8.78!!")
}

publishing {
    publications {
        create<MavenPublication>("zkGenerator") {
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
