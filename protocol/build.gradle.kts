plugins {
    kotlin("jvm")
    id("idea")
    id("maven-publish")
    id("java-library")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
    jacoco
}

dependencies {
    // These are required for our implementation and included in the JAR, we also expose their API for modules that depend on us.
    api(project(":utils"))
    api(project(":common"))
    api(project(":crypto"))
    api(project(":serialization"))
    api(project(":annotations"))

    // For zinc name
    implementation(project(":zinc-poet:zinc-bfl"))

    // Corda dependencies.
    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    compileOnly("$cordaReleaseGroup:corda-core:$cordaVersion")
    compileOnly("$cordaReleaseGroup:corda-node:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda:$cordaVersion")
    compileOnly("$cordaReleaseGroup:corda-test-utils:$cordaVersion")

    val log4jVersion: String by project
    compileOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")
    testImplementation(project(":test-utils"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.InternalSerializationApi"
}

tasks.shadowJar {
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains.kotlinx:kotlinx-serialization.*"))
    }
    archiveClassifier.set("")
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    publications {
        create<MavenPublication>("zkFlow") {
            project.shadow.component(this)
        }
    }
}
