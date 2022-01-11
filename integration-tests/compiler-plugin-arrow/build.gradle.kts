plugins {
    kotlin("jvm")
    id("java-library")
    kotlin("plugin.serialization")
    jacoco
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
    testImplementation(project(":protocol"))
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Generated class files are not recreated upon changes in compiler-plugin-arrow, therefor we always clean the
    // build, to enforce rebuild of classes with the updated compiler plugin.
    dependsOn("clean", ":compiler-plugin-arrow:assemble")
    mustRunAfter("clean", ":compiler-plugin-arrow:assemble")

    kotlinOptions {
        // IR backend is needed for Unsigned integer types support for kotlin 1.4, in $rootDir/build.gradle.kts:185 we
        // explicitly enforce 1.4.
        useIR = true
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xplugin=$rootDir/compiler-plugin-arrow/build/libs/compiler-plugin-arrow-$version.jar"
        freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    }
}
