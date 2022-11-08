plugins {
    kotlin("jvm")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization")
    jacoco
}

dependencies {
    implementation(project(":utils"))
    implementation(project(":crypto"))
    implementation(project(":annotations"))
    implementation(project(":serialization"))
    implementation(project(":zinc-poet:zinc-poet"))

    // For Witness JSON serialization
    val kotlinxSerializationVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    val cordaVersion: String by project
    val cordaReleaseGroup: String by project
    compileOnly("$cordaReleaseGroup:corda-core:$cordaVersion")
    compileOnly("$cordaReleaseGroup:corda-node:$cordaVersion")

    testImplementation(project(":test-utils"))

    testImplementation("$cordaReleaseGroup:corda-core:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
}

kotlin {
    // explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
}

publishing {
    publications {
        create<MavenPublication>("zkCommon") {
            from(components["java"])
        }
    }
}
