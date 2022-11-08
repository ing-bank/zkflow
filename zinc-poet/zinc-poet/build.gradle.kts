plugins {
    kotlin("jvm")
    id("java-library")
    id("maven-publish")
    jacoco
}

dependencies {
    implementation(project(":utils"))

    val kotlinxSerializationVersion: String by project
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}

publishing {
    publications {
        create<MavenPublication>("zkZincPoetPoet") {
            from(components["java"])
        }
    }
}
