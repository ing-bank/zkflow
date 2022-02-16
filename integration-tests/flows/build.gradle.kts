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
    testImplementation(project(":test-utils"))
    testImplementation(project(":protocol"))

    val cordaVersion: String by project
    testImplementation("net.corda:corda-core:$cordaVersion")
    testImplementation("net.corda:corda-node-driver:$cordaVersion")

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
