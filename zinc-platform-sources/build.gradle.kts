import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import kotlin.streams.toList

plugins {
    kotlin("jvm")
    java
    id("maven-publish")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":notary")) // required for Zinc com.ing.zknotary.gradle.zinc.template renderer
    implementation(project(":gradle-plugin"))

    val zkkryptoVersion: String by project
    testImplementation("com.ing.dlt:zkkrypto:$zkkryptoVersion")

    val kotlinxSerializationVersion: String by project
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    val kotlinxSerializationBflVersion: String by project
    testImplementation("com.ing.serialization.bfl:kotlinx-serialization-bfl:$kotlinxSerializationBflVersion")

    testImplementation(project(":test-utils"))
}

publishing {
    publications {
        create<MavenPublication>("zkGenerator") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ingzkp/zk-notary")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

kotlin.sourceSets {
    main {
        kotlin.srcDirs += File("src/main/kotlin")
    }
}

val root = "${project.rootDir.absolutePath}/zinc-platform-sources"

val circuitSourcesBase = File("$root/circuits")
val mergedCircuitOutput = File("$root/build/circuits")

val circuits = circuitSourcesBase.listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }
val configFileName = "config.json"

task("prepareCircuits", JavaExec::class) {
    inputs.dir(projectDir.resolve("src/main/resources"))
    inputs.dir(circuitSourcesBase)
    outputs.dir(mergedCircuitOutput)
    main = "PrepareCircuitsKt"
    classpath = sourceSets["main"].runtimeClasspath
    args(root, project.version, configFileName)
}

task("circuits") {
    mustRunAfter("rustfmtCheck")
    dependsOn("prepareCircuits")
}

task("rustfmt") {
    circuits?.forEach {
        outputs.dir("${mergedCircuitOutput.resolve(it)}/")
        outputs.files.forEach { file ->
            if (file.name.contains(".zn")) {
                exec {
                    commandLine("rustfmt", file)
                }
            }
        }
    }
}

task("rustfmtCheck") {
    mustRunAfter("prepareCircuits")
    circuits?.forEach {
        outputs.dir("${mergedCircuitOutput.resolve(it)}/")
        outputs.files.forEach { file ->
            if (file.name.contains(".zn")) {
                exec {
                    commandLine("rustfmt", "--check", it)
                }
            }
        }
    }
}

open class CopyCircuitTask @Inject constructor() : DefaultTask() {
    init {
        this.dependsOn(":zinc-platform-sources:circuits")
    }

    private val projectDir: Path = Paths.get(project.projectDir.absolutePath)

    @org.gradle.api.tasks.InputDirectory
    val resourcesDir: Path = projectDir.resolve("src/main/resources")

    @org.gradle.api.tasks.OutputDirectory
    val generatedResourcesDir: Path = projectDir.resolve("build/resources/test")

    @org.gradle.api.tasks.InputDirectory
    val testClassesPath: Path = projectDir.resolve("src/test/kotlin/com/ing/zknotary/zinc/types/")

    private fun findTestsInSubdirectory(rootDir: File, subDir: String? = null): List<String> {
        val baseDir = subDir?.let { rootDir.resolve(it) } ?: rootDir
        return (baseDir.listFiles() ?: emptyArray())
            .flatMap { file ->
                val filePath = (subDir?.let { it + File.separator } ?: "") + file.name
                when {
                    file.isDirectory -> findTestsInSubdirectory(rootDir, filePath)
                    file.name.endsWith("Test.kt") -> listOf(filePath)
                    else -> emptyList()
                }
            }
            .toList()
    }

    @TaskAction
    fun copy() {
        // In this package we test functions of classes, which are stored in one particular file,
        // but since there are many of them, we need many main functions with different parameters and output,
        // thus we copy implementation to each testing module in resources (because, zinc support modules pretty bad).
        findTestsInSubdirectory(testClassesPath.toFile())
            .map { it.removeSuffix(".kt") }
            .filter { Files.exists(generatedResourcesDir.resolve(it)) }
            .forEach { testClass ->
                val generatedTestSourceDir = generatedResourcesDir.resolve("$testClass/src/")
                listOf(
                    Files.list(project.buildDir.resolve("zinc-platform-test-sources").toPath()),
                    Files.list(resourcesDir.resolve("zinc-platform-libraries")),
                    listOf(resourcesDir.resolve("zinc-platform-sources").resolve("platform_consts.zn")).stream()
                )
                    .flatMap { it.toList() }
                    .filter { it.toString().endsWith(".zn") }
                    .forEach { testSource ->
                        val target = generatedTestSourceDir.resolve(testSource.fileName)
                        Files.copy(testSource, target, StandardCopyOption.REPLACE_EXISTING)
                    }
            }
    }
}

tasks.register<CopyCircuitTask>("copyCircuit")

tasks.processTestResources {
    finalizedBy(tasks.findByPath("copyCircuit"))
}
