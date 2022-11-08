plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    val cordaVersion: String by project
    compileOnly("net.corda:corda-core:$cordaVersion")
}

publishing {
    publications {
        create<MavenPublication>("zkAnnotations") {
            from(components["java"])
        }
    }
}
