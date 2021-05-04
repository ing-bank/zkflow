import java.nio.file.Files
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
    implementation(project(":notary")) // required for Zinc com.ing.zknotary.gradle.zinc.template rendere

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

task("prepareCircuits", JavaExec::class) {
    main = "PrepareCircuitsKt"
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
        dependsOn(":zinc-platform-sources:circuits")
    }

    @TaskAction
    fun copy() {
        // In this package we test functions of classes, which are stored in one particular file,
        // but since there are many of them, we need many main functions with different parameters and output,
        // thus we copy implementation to each testing module in resources (because, zinc support modules pretty bad).
        val projectDir = Paths.get(project.projectDir.absolutePath)
        val testClassesPath = projectDir.resolve("src/test/kotlin/com/ing/zknotary/zinc/types/")
        val resourcesDir = projectDir.resolve("src/main/resources")
        val generatedResourcesDir = projectDir.resolve("build/resources/test")
        Files.newDirectoryStream(testClassesPath)
            .map { it.fileName.toString().removeSuffix(".kt") }
            .filter { Files.exists(generatedResourcesDir.resolve(it)) }
            .forEach { testClass ->
                val generatedTestSourceDir = generatedResourcesDir.resolve("$testClass/src/")
                listOf(
                    Files.list(resourcesDir.resolve("zinc-platform-test-sources")),
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
