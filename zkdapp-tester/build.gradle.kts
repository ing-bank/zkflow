import com.ing.zkflow.gradle.task.CreateZincDirectoriesForCircuitTask

plugins {
    kotlin("jvm") version "1.5.31"
    id("com.ing.zkflow.gradle-plugin")
}

zkp {
    zkFlowTemplateConfigurationClass = "com.example.ZKFlowTemplateConfiguration"
}

repositories {
    google()
    maven("https://jitpack.io")
    maven("https://software.r3.com/artifactory/corda")
    maven("https://repo.gradle.org/gradle/libs-releases")
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.ing.zkflow:test-utils:1.0-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}

// Normally this build is run after the included build, but this ensures that all dependencies
// exist at build time. This is not necessary when using the real maven dependencies instead
// of a composite build.
tasks.matching {
    it.name == "processZincSources" ||
        it is CreateZincDirectoriesForCircuitTask
//            it is com.ing.zkflow.gradle.task.CopyZincCircuitSourcesTask ||
//            it is com.ing.zkflow.gradle.task.CopyZincPlatformSourcesTask ||
//            it is com.ing.zkflow.gradle.task.GenerateZincPlatformCodeFromTemplatesTask ||
//            it is com.ing.zkflow.gradle.task.PrepareCircuitForCompilationTask
}.forEach {
    val parentProject = gradle.includedBuild(project.rootDir.parentFile.name)
    it.mustRunAfter(parentProject.task(":zinc-platform-sources:assemble"))
    it.dependsOn(parentProject.task(":zinc-platform-sources:assemble"))
}
