plugins {
    kotlin("jvm")
    id("java-library")
    kotlin("plugin.serialization")
}

// This will prevent conflicts for between the original artifacts and their tests.
group = "$group.integration"

repositories {
    maven("https://software.r3.com/artifactory/corda")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    testImplementation(project(":annotations"))
    testImplementation(project(":test-utils"))
    testImplementation(project(":serialization-candidate"))

    val cordaVersion: String by project
    testImplementation("net.corda:corda-core:$cordaVersion")

    val kotlinxSerializationVersion: String by project
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    kotlinCompilerPluginClasspath(project(":utils"))
    kotlinCompilerPluginClasspath(project(":annotations"))
    kotlinCompilerPluginClasspath(project(":serialization-candidate"))
    kotlinCompilerPluginClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$kotlinxSerializationVersion")

    val arrowMetaVersion: String by project
    kotlinCompilerPluginClasspath("io.arrow-kt:arrow-meta:$arrowMetaVersion")
}

tasks {
    compileTestKotlin {
        dependsOn += ":compiler-plugin-arrow:assemble"
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += "-Xplugin=$rootDir/compiler-plugin-arrow/build/libs/compiler-plugin-arrow-$version.jar"
        }
    }
}
