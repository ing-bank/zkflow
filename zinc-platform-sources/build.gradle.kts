import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.streams.toList

plugins {
    kotlin("jvm")
    id("maven-publish")
    kotlin("plugin.serialization") // Only used for tests. Consider moving tests to a separate module for speed?
}

dependencies {
    implementation(project(":compilation")) // Only used for PrepareCircuits.kt, which is only used for test circuits
    implementation(project(":obsolete")) // TODO: Required only for CircuitConfigurator. Remove when that is removed.

    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    compileOnly("$cordaReleaseGroup:corda-core:$cordaVersion")

    testImplementation(project(":protocol"))
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
            url = uri("https://maven.pkg.github.com/ingzkp/zkflow")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
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

task("prepareCircuits", JavaExec::class) {
    inputs.dir(projectDir.resolve("src/main/resources"))
    inputs.dir(circuitSourcesBase)
    outputs.dir(mergedCircuitOutput)
    main = "com.ing.zkflow.PrepareCircuitsKt"
    classpath = sourceSets["main"].runtimeClasspath
    args(root, project.version)
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

    @org.gradle.api.tasks.OutputDirectory
    val generatedCircuitsCreateDir: Path = projectDir.resolve("build/circuits/create/src")

    @org.gradle.api.tasks.InputDirectory
    val testClassesPath: Path = projectDir.resolve("src/test/kotlin/com/ing/zkflow/zinc/types/")

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
                    listOf("platform_consts.zn", "platform_component_group_enum.zn")
                        .map { resourcesDir.resolve("zinc-platform-sources").resolve(it) }
                        .stream(),
                    listOf("test_state.zn", "tx_state_test_state.zn")
                        .map { generatedCircuitsCreateDir.resolve(it) }
                        .stream()
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
