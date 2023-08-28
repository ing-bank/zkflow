plugins {
    kotlin("jvm")
    id("java-library")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version "1.5.31-1.0.0"
}

// This will prevent conflicts for between the original artifacts and their tests.
group = "$group.integration"

repositories {
    maven("https://download.corda.net/maven/corda")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    testImplementation(project(":test-utils"))
    testImplementation(project(":protocol"))
    testImplementation(project(":integration-tests:fixtures"))
    testImplementation(project(":zinc-poet:zinc-code-generation"))

    val kotlinxSerializationVersion: String by project
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // We expect Corda to be on the class path for any CorDapp that uses ZKFlow.
    val cordaVersion: String by project
    val cordaReleaseGroup: String by project
    compileOnly("$cordaReleaseGroup:corda-core:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-core:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")

    // Normally, on real CorDapps, all the below deps would be set by the ZKFlow gradle plugin.
    // In the case of integration tests, we do it by hand.
    kotlinCompilerPluginClasspath("net.corda:corda-core:$cordaVersion")
    kotlinCompilerPluginClasspath(project(":utils"))
    kotlinCompilerPluginClasspath(project(":annotations"))
    kotlinCompilerPluginClasspath(project(":serialization"))
    kotlinCompilerPluginClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$kotlinxSerializationVersion")

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
    }
}
