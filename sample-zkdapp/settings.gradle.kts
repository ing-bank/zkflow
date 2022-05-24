pluginManagement {
    repositories {
        google()
        maven("https://plugins.gradle.org/m2/")
        maven("https://software.r3.com/artifactory/corda")
        maven("https://jitpack.io")

        maven {
            name = "BinaryFixedLengthSerializationRepo"
            url = uri("https://maven.pkg.github.com/ingzkp/kotlinx-serialization-bfl")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
includeBuild("..")
