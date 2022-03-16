plugins {
    kotlin("jvm")
    id("java-library")
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
    implementation(project(":test-utils"))
    implementation(project(":protocol"))
    implementation(project(":zinc-poet:zinc-code-generation"))

    val cordaVersion: String by project
    compileOnly("net.corda:corda-core:$cordaVersion")

    val kotlinxSerializationVersion: String by project
    kotlinCompilerPluginClasspath("net.corda:corda-core:$cordaVersion")
    kotlinCompilerPluginClasspath(project(":utils"))
    kotlinCompilerPluginClasspath(project(":annotations"))
    kotlinCompilerPluginClasspath(project(":serialization"))
    kotlinCompilerPluginClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$kotlinxSerializationVersion")

    val arrowMetaVersion: String by project
    kotlinCompilerPluginClasspath("io.arrow-kt:arrow-meta:$arrowMetaVersion")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
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
