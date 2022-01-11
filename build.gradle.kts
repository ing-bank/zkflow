import java.io.ByteArrayOutputStream

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
            maven("https://plugins.gradle.org/m2/")
            maven("https://oss.sonatype.org/content/repositories/snapshots/") // for arrow-meta

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

    @Suppress("UNCHECKED_CAST") this.repositories(repos as groovy.lang.Closure<Any>)

}

plugins {
    java
    idea
    kotlin("jvm") apply false
    id("com.diffplug.spotless") apply false
    id("io.gitlab.arturbosch.detekt")
    id("org.owasp.dependencycheck") version "6.1.1"
    jacoco
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
    val requiredZincVersion = projectDir.resolve(".github/workflows/on-push.yml").readLines().filter { it.matches(zincVersionRegex) }
        .map { it.replace(zincVersionRegex, "$1") }.single()
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
val testReport = tasks.register<TestReport>("testReport") {
    destinationDir = file("$buildDir/reports/tests/test")
    reportOn(subprojects.flatMap {
        it.tasks.matching { task -> task is Test }.map { test -> test as Test; test.binaryResultsDirectory }
    })
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

val detektAll by tasks.registering(io.gitlab.arturbosch.detekt.Detekt::class) {
    description = "Run detekt over whole code base without the starting overhead for each module."
    // Target version of the generated JVM bytecode. It is used for type resolution.
    jvmTarget = "1.8"
    config.setFrom("${rootDir}/config/detekt/detekt.yml")

    parallel = true

    source(files(projectDir))
    include("**/*.kt")
    exclude("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")

    reports {
        xml.enabled = false
        html.enabled = true
    }
}

jacoco {
    val jacocoToolVersion: String by project
    toolVersion = jacocoToolVersion
}

tasks.register("jacocoRootReport", JacocoReport::class) {
    val subProjectReportTasks = subprojects.map { it.tasks.withType<JacocoReport>() }
    val subprojectSourceDirs = subProjectReportTasks.flatMap { it.map { report -> report.sourceDirectories } }
    val subprojectAdditionalSourceDirs = subProjectReportTasks.flatMap { it.map { report -> report.additionalSourceDirs } }
    val subProjectClassDirectories = subProjectReportTasks.flatMap { it.map { report -> report.classDirectories } }
    val subProjectExecutionData = subProjectReportTasks.flatMap { it.map { report -> report.executionData } }

    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    dependsOn(subprojects.map { subProjectReportTasks })

    additionalSourceDirs.setFrom(subprojectAdditionalSourceDirs)
    sourceDirectories.setFrom(subprojectSourceDirs)
    classDirectories.setFrom(subProjectClassDirectories)
    executionData.setFrom(subProjectExecutionData)

    reports {
        xml.isEnabled = true
        html.isEnabled = true
        xml.destination = file("${buildDir}/reports/jacoco/aggregate/jacocoTestReport.xml")
        html.destination = file("${buildDir}/reports/jacoco/aggregate/html")
    }
    setOnlyIf { true }
}

tasks.register("jacocoRootCoverageVerification", JacocoCoverageVerification::class) {
    val subProjectReportTasks = subprojects.map { it.tasks.withType<JacocoReport>() }
    val subprojectSourceDirs = subProjectReportTasks.flatMap { it.map { report -> report.sourceDirectories } }
    val subprojectAdditionalSourceDirs = subProjectReportTasks.flatMap { it.map { report -> report.additionalSourceDirs } }
    val subProjectClassDirectories = subProjectReportTasks.flatMap { it.map { report -> report.classDirectories } }
    val subProjectExecutionData = subProjectReportTasks.flatMap { it.map { report -> report.executionData } }
    additionalSourceDirs.setFrom(subprojectAdditionalSourceDirs)
    sourceDirectories.setFrom(subprojectSourceDirs)
    classDirectories.setFrom(subProjectClassDirectories)
    executionData.setFrom(subProjectExecutionData)

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

subprojects {
    val repos: groovy.lang.Closure<RepositoryHandler> by rootProject.extra
    repositories(repos)

    val subproject = this

    // If a subproject has the Java plugin loaded, we set the test config on it.
    plugins.withType(JavaPlugin::class.java) {
        // Make sure the project has the necessary plugins loaded
        plugins.apply {
            apply("com.diffplug.spotless")
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
                target("*.gradle.kts")
                val ktlintVersion: String by project
                ktlint(ktlintVersion)
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

        this@subprojects.afterEvaluate {
            subproject.extensions.findByType(JacocoPluginExtension::class)?.let {
                val jacocoToolVersion: String by project
                it.toolVersion = jacocoToolVersion
            }
        }

        fun ConfigurableFileCollection.exclude(excludes: List<String>) = setFrom(files(files.map { fileTree(it) { exclude(excludes) } }))
        val jacocoConfigExcludesFile = "${rootProject.rootDir}/config/jacoco/excludes"
        val jacocoExcludes = File(jacocoConfigExcludesFile).readLines().filterNot { it.startsWith("#") }

        // This must be afterEvaluate, otherwise the excludes won't be applied.
        this@subprojects.afterEvaluate {
            this.tasks.withType<JacocoCoverageVerification> {
                sourceDirectories.exclude(jacocoExcludes)
                classDirectories.exclude(jacocoExcludes)
                additionalClassDirs.exclude(jacocoExcludes)
            }
        }

        // This must be afterEvaluate, otherwise the excludes won't be applied.
        this@subprojects.afterEvaluate {
            this.tasks.withType<JacocoReport> {
                sourceDirectories.exclude(jacocoExcludes)
                classDirectories.exclude(jacocoExcludes)
                additionalClassDirs.exclude(jacocoExcludes)
            }
        }

        this@subprojects.tasks.apply {
            matching { it is JavaCompile || it is org.jetbrains.kotlin.gradle.tasks.KotlinCompile }.forEach {
                it.dependsOn(":checkJavaVersion")
                it.dependsOn("spotlessApply") // Autofix before check
                it.dependsOn("spotlessCheck") // Fail on remaining non-autofixable issues
                it.dependsOn(":detektAll")
            }

            withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                kotlinOptions {
                    languageVersion = "1.5"
                    apiVersion = "1.4"
                    jvmTarget = "1.8"
                    javaParameters = true   // Useful for reflection.
                    freeCompilerArgs = listOf(
                        "-Xjvm-default=compatibility"
                    )
                    allWarningsAsErrors = true
                }
            }

            withType<Jar> {
                // This makes the JAR's SHA-256 hash repeatable.
                isPreserveFileTimestamps = false
                isReproducibleFileOrder = true
            }

            // This applies to all test types, both fast and slow
            withType<Test> {
                dependsOn(":checkZincVersion")
//                dependsOn(":zinc-platform-sources:circuits") // Make sure that the Zinc circuit is ready to use when running tests

                val cores = Runtime.getRuntime().availableProcessors()
                setForkEvery(100)
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
                    "java.util.logging.config.file", "${project.buildDir}/resources/test/logging-test.properties"
                )
            }

            matching { it is Test && it.name == "test" }.forEach { test ->
                test as Test
                test.useJUnitPlatform()

                test.extensions.findByType(JacocoTaskExtension::class)?.let {
                    it.isEnabled = false
                }
            }
        }
    }
}