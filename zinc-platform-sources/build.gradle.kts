plugins {
    kotlin("jvm")
    java
    id("maven-publish")
}

dependencies {
    implementation(project(":gradle-plugin"))
    implementation(project(":notary"))

    val zkkryptoVersion: String by project
    implementation("com.ing.dlt:zkkrypto:$zkkryptoVersion")
    // ZKP dependencies
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

val circuitSourcesBasePath = "$root/circuits"
val mergedCircuitOutputPath = "$root/build/circuits"

val circuitSourcesBase = File(circuitSourcesBasePath)
val mergedCircuitOutput = File(mergedCircuitOutputPath)

val circuits = circuitSourcesBase.listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }

// project.tasks.create("copyZincSources", CopyZincCircuitSourcesTask::class)
task("copyZincSources") {
    val zincPlatformSources = File("$root/src/main/resources/zinc-platform-sources")

    circuits?.forEach { circuitName ->
        // Copy circuit sources
        copy {
            from(circuitSourcesBase.resolve(circuitName))
            into(mergedCircuitOutput.resolve(circuitName).resolve("src"))
        }
        // Copy platform sources
        copy {
            from(zincPlatformSources)
            into(mergedCircuitOutput.resolve(circuitName).resolve("src"))
        }
    }
}

task("prepareCircuits", JavaExec::class) {
    main = "PrepareCircuitsKt"
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(root)
}

task("circuits") {
    mustRunAfter("rustfmtCheck")
    dependsOn("copyZincSources",
        "prepareCircuits")

    doLast {
        circuits?.forEach { circuitName ->
            val circuitPath = File("$mergedCircuitOutputPath/$circuitName")
            // Create Zargo.toml
            val zargoFile = File("${circuitPath.absolutePath}/Zargo.toml")
            zargoFile.delete()
            zargoFile.parentFile.mkdirs()
            zargoFile.createNewFile()
            zargoFile.writeText(
                """
    [circuit]
    name = "$circuitName"
    version = "${project.version}"                                
"""
            )

            // Compile circuit
            exec {
                workingDir = circuitPath
                executable = "zargo"
                args = listOf("clean", "-v")
            }
        }
    }
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

open class CopyCircuitTask : DefaultTask() {

    init {
        dependsOn(":zinc-platform-sources:circuits")
    }

    @TaskAction
    fun copy() {

        // In this package we test functions of classes, which are stored in one particular file,
        // but since there are many of them, we need many main functions with different parameters and output,
        // thus we copy implementation to each testing module in resources (because, zinc support modules pretty bad).
        val testClassesPath = `java.nio.file`.Paths.get("zinc-platform-sources/src/test/kotlin/zinc/types/")
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
