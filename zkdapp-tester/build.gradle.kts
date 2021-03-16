import com.ing.zknotary.gradle.task.CopyZincCircuitSourcesTask
import com.ing.zknotary.gradle.task.CopyZincPlatformSourcesTask
import com.ing.zknotary.gradle.task.CreateZincDirectoriesForInputCommandTask
import com.ing.zknotary.gradle.task.GenerateZincPlatformCodeFromTemplatesTask
import com.ing.zknotary.gradle.task.PrepareCircuitForCompilationTask

plugins {
    kotlin("jvm") version "1.4.20"
    id("com.ing.zknotary.gradle-plugin")
}

zkp {
    bigDecimalSizes = setOf(Pair(25, 6), Pair(102, 20))
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


tasks.create("processZincSources") {
    mustRunAfter(gradle.includedBuild("zk-notary").task(":zinc-platform-sources:assemble"))
    dependsOn(gradle.includedBuild("zk-notary").task(":zinc-platform-sources:assemble"))

    tasks.matching {
        it is CopyZincCircuitSourcesTask ||
                it is CopyZincPlatformSourcesTask ||
                it is GenerateZincPlatformCodeFromTemplatesTask ||
                it is PrepareCircuitForCompilationTask

    }.forEach {
        it.mustRunAfter(gradle.includedBuild("zk-notary").task(":zinc-platform-sources:assemble"))
        it.dependsOn(gradle.includedBuild("zk-notary").task(":zinc-platform-sources:assemble"))

        it.mustRunAfter("createZincDirectoriesForInputCommand")
    }

    tasks.withType(CreateZincDirectoriesForInputCommandTask::class) {
        mustRunAfter(gradle.includedBuild("zk-notary").task(":zinc-platform-sources:assemble"))
        dependsOn(gradle.includedBuild("zk-notary").task(":zinc-platform-sources:assemble"))

    }

    dependsOn("copyZincPlatformSources")
    dependsOn("generateZincPlatformCodeFromTemplates")
    dependsOn("prepareCircuitForCompilation")
    dependsOn("copyZincCircuitSources")
}
