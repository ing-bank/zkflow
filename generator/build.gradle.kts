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
    // Required to override everything included by kotlin-compile-testing
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
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
