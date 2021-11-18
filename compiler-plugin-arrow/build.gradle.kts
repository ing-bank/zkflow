plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("maven-publish")
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    val arrowMetaVersion: String by project
    implementation("io.arrow-kt:arrow-meta:$arrowMetaVersion")

    implementation(project(":serialization-candidate"))
    implementation(project(":annotations"))
    implementation(project(":utils"))
}

tasks {
    jar {
        val log = File("/tmp/apply-plugin.log")
        log.appendText("Jar task compiler-plugin-arrow\n")

        dependsOn(":utils:assemble")
        dependsOn(":serialization-candidate:assemble")

        val toInclude = listOf(
            "utils",
            "annotations",
            "serialization-candidate",
            "kotlinx-serialization-core-jvm",
            "arrow-meta"
        )

        from({
            configurations.runtimeClasspath.get()
                .filter {
                    val include = toInclude.any { inclusion -> it.name.startsWith(inclusion) }
                    log.appendText("[${if (include) "X" else "0"}] ${it.name}\n\t${it.absolutePath}\n")
                    include
                }
                .map { if (it.isDirectory) it else zipTree(it) }
        })

        from({
            configurations.compileClasspath.get()
                .filter {
                    val include = toInclude.any { inclusion -> it.name.startsWith(inclusion) }
                    log.appendText("[${if (include) "X" else "0"}] ${it.absolutePath}\n")
                    include
                }
                .map { if (it.isDirectory) it else zipTree(it) }
        })
    }
}

publishing {
    publications {
        create<MavenPublication>("arrowCompilerPlugin") {
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
