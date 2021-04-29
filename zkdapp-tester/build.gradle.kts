import com.ing.zknotary.gradle.task.CreateZincDirectoriesForInputCommandTask
import com.ing.zknotary.gradle.template.BigDecimalTemplateParameters
import com.ing.zknotary.gradle.template.AmountTemplateParameters
import com.ing.zknotary.gradle.template.StringTemplateParameters

plugins {
    kotlin("jvm") version "1.4.20"
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

    // Temporary ING fork of Corda
    maven {
        name = "CordaForkRepo"
        url = uri("https://maven.pkg.github.com/ingzkp/corda")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
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
    it.mustRunAfter(gradle.includedBuild("zk-notary").task(":zinc-platform-sources:assemble"))
    it.dependsOn(gradle.includedBuild("zk-notary").task(":zinc-platform-sources:assemble"))
}
