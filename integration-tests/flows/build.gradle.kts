plugins {
    kotlin("jvm")
    id("net.corda.plugins.quasar-utils")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version "1.5.31-1.0.0"
}

// This will prevent conflicts for between the original artifacts and their tests.
group = "$group.integration"

repositories {
    maven("https://software.r3.com/artifactory/corda")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    // implementation(project(":annotations"))
    implementation(project(":test-utils"))
    implementation(project(":protocol"))
    // implementation(project(":serialization-candidate"))

    val cordaVersion: String by project
    implementation("net.corda:corda-core:$cordaVersion")

    // val kotlinxSerializationVersion: String by project
    // implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    // kotlinCompilerPluginClasspath(project(":utils"))
    // kotlinCompilerPluginClasspath(project(":annotations"))
    // kotlinCompilerPluginClasspath(project(":serialization-candidate"))
    // kotlinCompilerPluginClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$kotlinxSerializationVersion")
    //
    // val arrowMetaVersion: String by project
    // kotlinCompilerPluginClasspath("io.arrow-kt:arrow-meta:$arrowMetaVersion")

    implementation(project(":compiler-plugin-ksp"))
    ksp(project(":compiler-plugin-ksp"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
