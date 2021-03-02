plugins {
    kotlin("jvm")
    id("com.ing.zknotary.gradle-plugin") version "0.1.4-SNAPSHOT"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.create("mergeZincTest") {
    dependsOn("clean")
    dependsOn(":zinc-platform-sources:publishToMavenLocal")
    dependsOn(":gradle-plugin:publishToMavenLocal")
    dependsOn("copyZincCircuitSources")
    dependsOn("copyZincPlatformSources")
}
