import com.ing.zknotary.gradle.task.CreateZincDirectoriesForInputCommandTask
import com.ing.zknotary.gradle.zinc.template.parameters.BigDecimalTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.AmountTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.StringTemplateParameters

plugins {
    kotlin("jvm") version "1.4.30"
    id("com.ing.zknotary.gradle-plugin")
}

zkp {
    stringConfigurations = listOf(StringTemplateParameters(33))
    bigDecimalConfigurations = listOf(
        BigDecimalTemplateParameters(25, 6),
        BigDecimalTemplateParameters(102, 20)
    )
    amountConfigurations = listOf(
        AmountTemplateParameters(BigDecimalTemplateParameters(25, 6), 8),
        AmountTemplateParameters(BigDecimalTemplateParameters(102, 20), 8)
    )
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
}

// Normally this build is run after the included build, but this ensures that all dependencies
// exist at build time. This is not necessary when using the real maven dependencies instead
// of a composite build.
tasks.matching {
    it.name == "processZincSources" ||
        it is CreateZincDirectoriesForInputCommandTask
//            it is com.ing.zknotary.gradle.task.CopyZincCircuitSourcesTask ||
//            it is com.ing.zknotary.gradle.task.CopyZincPlatformSourcesTask ||
//            it is com.ing.zknotary.gradle.task.GenerateZincPlatformCodeFromTemplatesTask ||
//            it is com.ing.zknotary.gradle.task.PrepareCircuitForCompilationTask
}.forEach {
    val parentProject = gradle.includedBuild(project.rootDir.parentFile.name)
    it.mustRunAfter(parentProject.task(":zinc-platform-sources:assemble"))
    it.dependsOn(parentProject.task(":zinc-platform-sources:assemble"))
}
