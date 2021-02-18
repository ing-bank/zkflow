buildscript {
    val repos by extra {
        closureOf<RepositoryHandler> {
            mavenLocal()
            google()
            mavenCentral()
            jcenter()
            maven("https://jitpack.io")
            maven("https://repo.gradle.org/gradle/libs-releases")
            maven("https://software.r3.com/artifactory/corda")

            maven {
                name = "CordaForkRepo"
                url = uri("https://maven.pkg.github.com/ingzkp/corda")
                credentials {
                    username = System.getenv("GITHUB_USERNAME")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
            maven {
                name = "KspForkRepo"
                url = uri("https://maven.pkg.github.com/ingzkp/ksp")
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

plugins {
    kotlin("jvm") apply false
    id("com.diffplug.gradle.spotless") apply false
    id("io.gitlab.arturbosch.detekt") apply false
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

    // If a subproject has the Java plugin loaded, we set the test config on it.
    plugins.withType(JavaPlugin::class.java) {
        // Make sure the project has the necessary plugins loaded
        plugins.apply {
            apply("com.diffplug.gradle.spotless")
            apply("io.gitlab.arturbosch.detekt")
        }

        // Load the necessary dependencies
        dependencies.apply {
            val kotlinVersion: String by project
            add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
            add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
            add("implementation", "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

            // Testing
            val junit5Version: String by project
            val kotestVersion: String by project

            add("testImplementation", "org.junit.jupiter:junit-jupiter-api:$junit5Version")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
            add("testImplementation", "io.kotest:kotest-assertions-core:$kotestVersion")
        }

        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("${buildDir.relativeTo(rootDir).path}/generated/**")
                val ktlintVersion: String by project
                ktlint(ktlintVersion)
            }
            kotlinGradle {
                target("*.gradle.kts") // default target for kotlinGradle
                ktlint() // or ktfmt() or prettier()
            }
        }

        this@subprojects.tasks.apply {
            matching { it is JavaCompile || it is org.jetbrains.kotlin.gradle.tasks.KotlinCompile }.forEach {
                it.dependsOn(":checkJavaVersion")
                it.dependsOn("spotlessApply") // Autofix before check
                it.dependsOn("spotlessCheck") // Fail on remaining non-autofixable issues
            }

            withType<io.gitlab.arturbosch.detekt.Detekt> {
                // Target version of the generated JVM bytecode. It is used for type resolution.
                jvmTarget = "1.8"
                config.setFrom("${rootDir}/config/detekt/detekt.yml")
            }


            withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                kotlinOptions {
                    languageVersion = "1.3"
                    apiVersion = "1.3"
                    jvmTarget = "1.8"
                    javaParameters = true   // Useful for reflection.
                    freeCompilerArgs = listOf("-Xjvm-default=compatibility")
                }
            }

            withType<Jar> {
                // This makes the JAR's SHA-256 hash repeatable.
                isPreserveFileTimestamps = false
                isReproducibleFileOrder = true
            }

            // This applies to all test types, both fast and slow
            withType<Test> {
                dependsOn("detekt")
                dependsOn(":prover:circuits") // Make sure that the Zinc circuit is ready to use when running tests

                val cores = Runtime.getRuntime().availableProcessors()
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
                logger.info("Using $cores cores to run $maxParallelForks test forks.")

                testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = true
                }

                val mockZKP = System.getProperty("MockZKP")
                if (mockZKP != null) {
                    systemProperty("MockZKP", true)
                }

                // Set the default log4j config file for tests
                systemProperty("log4j.configurationFile", "${project.buildDir}/resources/test/log4j2.xml")

                // Allow setting a custom log4j config file
                val logConfigPath = System.getProperty("log4j.configurationFile")
                if (logConfigPath != null) {
                    systemProperty("log4j.configurationFile", logConfigPath)
                }

                // This file determines for the standard java.util.logging how and what is logged to the console
                // This is to configure logging that does not go through slf4j/log4j, like JUnit platform logging.
                systemProperty(
                    "java.util.logging.config.file",
                    "${project.buildDir}/resources/test/logging-test.properties"
                )
            }

            task<Test>("slowTest") {
                val root = project.rootDir.absolutePath
                inputs.dir("$root/prover/circuits")

                useJUnitPlatform {
                    includeTags("slow")

                }
                shouldRunAfter("test")
            }

            task<Test>("nightlyTest") {
                val root = project.rootDir.absolutePath
                inputs.dir("$root/prover/circuits")

                useJUnitPlatform {
                    includeTags("nightly")

                }
                shouldRunAfter("test")
                shouldRunAfter("slowTest")
            }

            task<Test>("allTests") {
                dependsOn("test", "slowTest", "ciOnlyTest")
            }

            matching { it is Test && it.name == "test" }.forEach {
                it as Test
                // We have a separate task for slow tests
                it.useJUnitPlatform {
                    excludeTags("slow")
                }
            }
        }

    }
}


