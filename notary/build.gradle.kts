plugins {
    kotlin("jvm")
    id("idea")
    id("net.corda.plugins.cordapp")
    id("net.corda.plugins.quasar-utils")
    id("maven-publish")
    id("java-library")
}

cordapp {
    val platformVersion: String by project
    targetPlatformVersion.set(platformVersion.toInt())
    minimumPlatformVersion.set(platformVersion.toInt())
    workflow {
        name.set("Zk Notary App")
        vendor.set("ING Bank NV")
        licence.set("Apache License, Version 2.0")
        versionId.set(1)
    }
}

dependencies {
    // Corda dependencies.
    val cordaReleaseGroup: String by project
    val cordaVersion: String by project
    cordaCompile("$cordaReleaseGroup:corda-core:$cordaVersion")
    cordaRuntime("$cordaReleaseGroup:corda:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-node:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-jackson:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")

    val zkkryptoVersion: String by project
    implementation("com.ing.dlt:zkkrypto:$zkkryptoVersion")

    // ZKP dependencies
    testImplementation(project(":test-utils"))
}

val testConfigResourcesDir = "$rootDir/config/test"
sourceSets {
    test {
        resources {
            srcDir(testConfigResourcesDir)
        }
    }
}

// TODO: We will have to enable explicitApi soon:
// https://kotlinlang.org/docs/reference/whatsnew14.html#explicit-api-mode-for-library-authors
// kotlin {
//     explicitApi = Strict
// }

// Separate task is used in order to ensure copying will be done properly, because
// such implementation fails sometimes:
// tasks.withType<Test> {
//      beforeSuite {
//      }
// }

// TODO: This should probably become a fat jar at some point, with its dependencies included
publishing {
    publications {
        create<MavenPublication>("zkNotary") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ingzkp/zk-notary")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
