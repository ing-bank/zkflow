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
    testImplementation(project(":test-utils"))
    testImplementation(project(":protocol"))
    testImplementation(project(":integration-tests:fixtures"))

    val cordaVersion: String by project
    kotlinCompilerPluginClasspath("net.corda:corda-core:$cordaVersion")
    kotlinCompilerPluginClasspath(project(":utils"))
    kotlinCompilerPluginClasspath(project(":annotations"))
    kotlinCompilerPluginClasspath(project(":serialization"))

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        // IR backend is needed for Unsigned integer types support for kotlin 1.4, in $rootDir/build.gradle.kts:185 we
        // explicitly enforce 1.4.
        useIR = true
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    }
}
