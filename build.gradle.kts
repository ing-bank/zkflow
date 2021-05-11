import java.io.ByteArrayOutputStream

buildscript {
    val repos by extra {
        closureOf<RepositoryHandler> {
            mavenLocal()
            google()
            mavenCentral()
            jcenter()
            maven("https://dl.bintray.com/kotlin/kotlin-dev/")
            maven("https://jitpack.io")
            maven("https://repo.gradle.org/gradle/libs-releases")
            maven("https://software.r3.com/artifactory/corda")

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

    @Suppress("UNCHECKED_CAST")
    this.repositories(repos as groovy.lang.Closure<Any>)

}

plugins {
    java
    idea
    kotlin("jvm") apply false
    id("com.diffplug.spotless") apply false
    id("io.gitlab.arturbosch.detekt")
    id("org.owasp.dependencycheck") version "6.1.1"
}

repositories {
    jcenter()
}

dependencyCheck {
    suppressionFile = projectDir.resolve("config/owasp/suppressions.xml").absolutePath
    analyzers.apply {
        assemblyEnabled = false
        nodeEnabled = false
        retirejs.enabled = false
    }
    failBuildOnCVSS = 6.9F
}

task("checkJavaVersion") {
    if (!JavaVersion.current().isJava8) {
        throw IllegalStateException(
            "ERROR: Java 1.8 required but " + JavaVersion.current() + " found. Change your JAVA_HOME environment variable."
        )
    }
}

val zincVersionRegex = ".*ZINC_VERSION: \"v(.*)\".*".toRegex()
val zincVersionOutputRegex = "^znc (.*)$".toRegex()
task("checkZincVersion") {
    val requiredZincVersion = projectDir.resolve(".github/workflows/on-push.yml").readLines()
        .filter { it.matches(zincVersionRegex) }
        .map { it.replace(zincVersionRegex, "$1") }
        .single()
    ByteArrayOutputStream().use { os ->
        val result = exec {
            executable = "znc"
            args = listOf("--version")
            standardOutput = os
        }
        if (result.exitValue != 0) {
            throw IllegalStateException(
                "ERROR: Zinc was not found on this system, please install Zinc version '$requiredZincVersion'."
            )
        } else {
            val zincVersion = os.toString().trim().replace(zincVersionOutputRegex, "$1")
            if (zincVersion != requiredZincVersion) {
                throw IllegalStateException(
                    "ERROR: Zinc version '$requiredZincVersion' required, but '$zincVersion' found. Please update zinc."
                )
            }
        }
    }
}

// This task generates an aggregate test report from all subprojects
// EXCLUDING NIGHTLY TESTS
val testReport = tasks.register<TestReport>("testReport") {
    destinationDir = file("$buildDir/reports/tests/test")
    reportOn(subprojects.flatMap {
        it.tasks.matching { task -> task is Test && task.name != "nightlyTest" && task.name != "allTests" }
            .map { test -> test as Test; test.binaryResultsDirectory }
    })
}

// This task generates an aggregate test report from all subprojects
// INCLUDING NIGHTLY TESTS
val testReportAll = tasks.register<TestReport>("testReportAll") {
    destinationDir = file("$buildDir/reports/tests/test")
    reportOn(subprojects.flatMap {
        it.tasks.filterIsInstance<Test>()
            .map { test -> test as Test; test.binaryResultsDirectory }
    })
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt> {
    // Target version of the generated JVM bytecode. It is used for type resolution.
    jvmTarget = "1.8"
    config.setFrom("${rootDir}/config/detekt/detekt.yml")

    parallel = true

    source(files(rootProject.projectDir))
    include("**/*.kt")
    exclude("**/*.kts")
    exclude("**/resources/")
    exclude("**/build/")
    exclude("ivno/*")
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

subprojects {
    val repos: groovy.lang.Closure<RepositoryHandler> by rootProject.extra
    repositories(repos)

    // If a subproject has the Java plugin loaded, we set the test config on it.
    plugins.withType(JavaPlugin::class.java) {
        // Make sure the project has the necessary plugins loaded
        plugins.apply {
            apply("com.diffplug.spotless")
            /* TODO: Aggregated Jacoco report
             * https://docs.gradle.org/6.5.1/samples/sample_jvm_multi_project_with_code_coverage.html for aggregate coverage report
             * https://gist.github.com/tsjensen/d8b9ab9e6314ae2f63f4955c44399dad
             */
            apply("jacoco")
            apply("idea")
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
            add("testImplementation", "org.junit.jupiter:junit-jupiter-params:$junit5Version")
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

        val testConfigResourcesDir = "${rootProject.rootDir}/config/test"
        sourceSets {
            test {
                resources {
                    srcDir(testConfigResourcesDir)
                }
            }
        }

        this@subprojects.tasks.apply {
            matching { it is JavaCompile || it is org.jetbrains.kotlin.gradle.tasks.KotlinCompile }.forEach {
                it.dependsOn(":checkJavaVersion")
                it.dependsOn("spotlessApply") // Autofix before check
                it.dependsOn("spotlessCheck") // Fail on remaining non-autofixable issues
            }

            withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                kotlinOptions {
                    languageVersion = "1.4"
                    apiVersion = "1.4"
                    jvmTarget = "1.8"
                    javaParameters = true   // Useful for reflection.
                    freeCompilerArgs =
                        listOf(
                            "-Xjvm-default=compatibility",
                            "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi",
                            "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
                            "-Xopt-in=kotlin.ExperimentalTime"
                        )
                }
            }

            withType<Jar> {
                // This makes the JAR's SHA-256 hash repeatable.
                isPreserveFileTimestamps = false
                isReproducibleFileOrder = true
            }

            // This applies to all test types, both fast and slow
            withType<Test> {
                dependsOn(":detekt")
                dependsOn(":checkZincVersion")
                dependsOn(":zinc-platform-sources:circuits") // Make sure that the Zinc circuit is ready to use when running tests

                val cores = Runtime.getRuntime().availableProcessors()
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
                logger.info("Using $cores cores to run $maxParallelForks test forks.")

                testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = true
                }

                // Individual projects should not report, we aggregate all results for all projects
                reports.html.isEnabled = false

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
                inputs.dir("$root/zinc-platform-sources/circuits")

                useJUnitPlatform {
                    includeTags("slow")
                }
                shouldRunAfter("test")
            }

            task<Test>("nightlyTest") {
                val root = project.rootDir.absolutePath
                inputs.dir("$root/zinc-platform-sources/circuits")

                useJUnitPlatform {
                    includeTags("nightly")
                }
                shouldRunAfter("test")
                shouldRunAfter("slowTest")

                // If running OWASP checks, run it first, so the build fails faster
                mustRunAfter(":dependencyCheckAggregate")
            }

            task<Test>("allTests") {
                dependsOn("test", "slowTest", "nightlyTest")
            }

            matching { it is Test && it.name == "test" }.forEach {
                it as Test
                // We have a separate task for slow tests
                it.useJUnitPlatform {
                    excludeTags("slow")
                    excludeTags("nightly")
                }
            }
        }
    }
}
