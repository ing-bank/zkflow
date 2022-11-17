import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.render.ReportRenderer
import java.io.ByteArrayOutputStream

buildscript {
    val repos by extra {
        closureOf<RepositoryHandler> {
            mavenLocal()
            google()
            mavenCentral()
            jcenter()
            maven("https://repo.gradle.org/gradle/libs-releases")
            maven("https://software.r3.com/artifactory/corda")
            maven("https://plugins.gradle.org/m2/")
            maven("https://jitpack.io")
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
    id("com.github.spotbugs") version "4.8.0" apply false
    id("com.github.jk1.dependency-license-report") apply false
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

val zincVersionOutputRegex = "^znc (.*)$".toRegex()
task("checkZincVersion") {
    doLast {
        val zincVersion: String by project
        ByteArrayOutputStream().use { os ->
            val result = exec {
                executable = "znc"
                args = listOf("--version")
                standardOutput = os
                errorOutput = os
                isIgnoreExitValue = true
            }
            if (result.exitValue != 0) {
                throw IllegalStateException(
                    "ERROR: Zinc was not found on this system, please install Zinc version '$zincVersion'."
                )
            } else {
                val actualZincVersion = os.toString().trim().replace(zincVersionOutputRegex, "$1")
                if (actualZincVersion != zincVersion) {
                    throw IllegalStateException(
                        "ERROR: Zinc version '$zincVersion' required, but '$actualZincVersion' found. Please update zinc."
                    )
                }
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

    val subproject: Project = this

    // If a subproject has the Java plugin loaded, we set the test config on it.
    plugins.withType(JavaPlugin::class.java) {
        // Make sure the project has the necessary plugins loaded
        plugins.apply {
            apply("com.diffplug.spotless")
            apply("idea")
            apply("com.github.spotbugs")
        }

        // Load the necessary dependencies
        dependencies.apply {
            val kotlinVersion: String by project
            add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
            add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
            add("implementation", "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

            // Testing
            val junit5Version: String by project
            add("testImplementation", "org.junit.jupiter:junit-jupiter-api:$junit5Version")
            add("testImplementation", "org.junit.jupiter:junit-jupiter-params:$junit5Version")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:$junit5Version")
            val kotestVersion: String by project
            add("testImplementation", "io.kotest:kotest-assertions-core:$kotestVersion")
            // Mocking
            val mockkVersion: String by project
            add("testImplementation", "io.mockk:mockk:$mockkVersion")

            add("spotbugsPlugins", "com.h3xstream.findsecbugs:findsecbugs-plugin:1.11.0")
            add("compileOnly", "com.github.spotbugs:spotbugs-annotations:4.5.3")
            add("testCompileOnly", "com.github.spotbugs:spotbugs-annotations:4.5.3")
        }


        if (!subproject.path.startsWith(":integration-tests")) {
            plugins.apply {
                apply("com.github.jk1.dependency-license-report")
            }
            configure<LicenseReportExtension> {
                renderers = arrayOf<ReportRenderer>(
                    com.github.jk1.license.render.InventoryHtmlReportRenderer(),
                    com.github.jk1.license.render.InventoryReportRenderer()
                )
                filters = arrayOf<DependencyFilter>(
                    com.github.jk1.license.filter.LicenseBundleNormalizer(),
                    com.github.jk1.license.filter.ExcludeTransitiveDependenciesFilter()
                )
                allowedLicensesFile = rootProject.projectDir.resolve("config/allowed-licenses.json")
            }
        }

        subproject.java {
            @Suppress("UnstableApiUsage")
            withSourcesJar()
            @Suppress("UnstableApiUsage")
            withJavadocJar()
        }

        configure<com.github.spotbugs.snom.SpotBugsExtension> {
            toolVersion.set("4.5.3")
            showProgress.set(false)
            effort.set(com.github.spotbugs.snom.Effort.MAX)
            reportLevel.set(com.github.spotbugs.snom.Confidence.DEFAULT)
            includeFilter.set(rootProject.projectDir.resolve("config/spotbugs/spotbugs-security-include.xml"))
            excludeFilter.set(rootProject.projectDir.resolve("config/spotbugs/spotbugs-security-exclude.xml"))
            extraArgs.set(listOf("-quiet", "-longBugCodes"))
        }

        val spotbugsReport by tasks.registering {
            onlyIf {
                tasks.findByName("spotbugsMain")?.state?.failure != null
            }
            doLast {
                println()
                projectDir.resolve("build/reports/spotbugs/main.txt").readLines().forEach {
                    println(it)
                }
            }
        }

        tasks.getByName("spotbugsTest") {
            onlyIf { false } // Don't scan test code
        }

        tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
            reports {
                create("text") {
                    required.set(true)
                }
                create("html") {
                    required.set(false)
                    isEnabled = false
                }
                create("xml") {
                    isEnabled = false
                }
            }
            finalizedBy(spotbugsReport)
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
                    apiVersion = "1.3"
                    jvmTarget = "1.8"
                    javaParameters = true   // Useful for reflection.
                    freeCompilerArgs = listOf(
                        "-Xjvm-default=compatibility"
                    )
                    // allWarningsAsErrors = true
                }
            }

            withType<Jar> {
                // This makes the JAR's SHA-256 hash repeatable.
                isPreserveFileTimestamps = false
                isReproducibleFileOrder = true
                exclude("**/module-info.class")
                exclude("README.txt")
                exclude("LICENSE")
                exclude("LICENSE-junit.txt")
                exclude("log4j2-test.xml")
            }

            withType<Test> {
                dependsOn(":checkZincVersion")

                val cores = Runtime.getRuntime().availableProcessors()
                setForkEvery(100)
                maxParallelForks = (cores / 2).takeIf { it > 0 } ?: 1
                logger.info("Using $cores cores to run $maxParallelForks test forks.")

                maxHeapSize = "8192m"

                testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = true
                }

                // Individual projects should not report, we aggregate all results for all projects
                reports.html.isEnabled = false

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
