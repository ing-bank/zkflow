plugins {
    kotlin("jvm")
    id("idea")
    id("java-library")
    id("maven-publish")
}

dependencies {
    implementation(project(":protocol"))

    implementation(kotlin("stdlib"))

    val kspVersion: String by project
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    val kspTestingVersion: String by project
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:$kspTestingVersion")
}

publishing {
    publications {
        create<MavenPublication>("zkTransactionCompilerPlugin") {
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
