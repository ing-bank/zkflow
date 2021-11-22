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
        dependsOn(":utils:assemble")
        dependsOn(":serialization-candidate:assemble")

        val toInclude = listOf(
            "zkflow/utils",
            "zkflow/annotations",
            "zkflow/serialization-candidate",
            "org.jetbrains.kotlinx/kotlinx-serialization-core-jvm",
            "io.arrow-kt/arrow-meta"
        )

        from({
            (configurations.runtimeClasspath.get() + configurations.compileClasspath.get())
                .filter { dep ->
                    toInclude.any { inclusion -> dep.isFile && dep.canonicalPath.contains(inclusion) }
                }
                .distinctBy { it.canonicalPath }
                .map { dep ->
                    // ATTENTION.
                    // This logging information is printed every time Gradle finds it necessary to trigger `from`,
                    // thus resulting in a multiple repeating lines.
                    logger.quiet("Embedding ${dep.name}\n\t${dep.canonicalPath}")
                    zipTree(dep)
                }
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
