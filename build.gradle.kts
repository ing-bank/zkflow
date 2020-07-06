buildscript {
    val repos by extra {
        closureOf<RepositoryHandler> {
            mavenCentral()
            maven("https://jitpack.io")
            maven("https://repo.gradle.org/gradle/libs-releases")
            maven("https://software.r3.com/artifactory/corda")

            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/ingzkp/corda")
                credentials {
                    username = System.getenv("GITHUB_USERNAME")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    this.repositories(repos as groovy.lang.Closure<Any>)
}

task("checkJavaVersion") {
    if (!JavaVersion.current().isJava8) {
        throw IllegalStateException(
            "ERROR: Java 1.8 required but " + JavaVersion.current() + " found. Change your JAVA_HOME environment variable."
        )
    }
}

subprojects {
    val repos: groovy.lang.Closure<RepositoryHandler> by rootProject.extra
    repositories(repos)
}


