import javax.inject.Inject

plugins {
    kotlin("jvm")
    java
    id("maven-publish")
}

dependencies {
    implementation(project(":gradle-plugin"))

    testImplementation(project(":notary"))

    val zkkryptoVersion: String by project
    testImplementation("com.ing.dlt:zkkrypto:$zkkryptoVersion")

    val kotlinxSerializationVersion: String by project
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    val kotlinxSerializationBflVersion: String by project
    testImplementation("com.ing.serialization.bfl:kotlinx-serialization-bfl:$kotlinxSerializationBflVersion")

    testImplementation(project(":test-utils"))

    testImplementation(project(":notary"))
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
        val testClassesPath =
            `java.nio.file`.Paths.get(project.projectDir.resolve("src/test/kotlin/zinc/types/").absolutePath)
        `java.nio.file`.Files.newDirectoryStream(testClassesPath)
            .map { it.fileName.toString().removeSuffix(".kt") }
            .filter { `java.nio.file`.Files.exists(`java.nio.file`.Paths.get("zinc-platform-sources/build/resources/test/$it")) }
            .forEach {
                `java.nio.file`.Files.copy(
                    `java.nio.file`.Paths.get("zinc-platform-sources/src/main/resources/zinc-platform-test-sources/floating_point_24_6.zn"),
                    `java.nio.file`.Paths.get("zinc-platform-sources/build/resources/test/$it/src/floating_point_24_6.zn"),
                    `java.nio.file`.StandardCopyOption.REPLACE_EXISTING
                )
                `java.nio.file`.Files.copy(
                    `java.nio.file`.Paths.get("zinc-platform-sources/src/main/resources/zinc-platform-test-sources/floating_point_100_20.zn"),
                    `java.nio.file`.Paths.get("zinc-platform-sources/build/resources/test/$it/src/floating_point_100_20.zn"),
                    `java.nio.file`.StandardCopyOption.REPLACE_EXISTING
                )
            }
    }
}

tasks.register<CopyCircuitTask>("copyCircuit")

tasks.processTestResources {
    finalizedBy(tasks.findByPath("copyCircuit"))
}
