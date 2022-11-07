plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
}

kotlin {
    // explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
}

dependencies {
    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    compileOnly("$cordaReleaseGroup:corda-core:$cordaVersion")

    val zkkryptoVersion: String by project
    api("com.ing.dlt:zkkrypto:$zkkryptoVersion")

    implementation(project(":annotations"))
}

publishing {
    publications {
        create<MavenPublication>("zkCrypto") {
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
