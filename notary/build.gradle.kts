plugins {
    kotlin("jvm")
    id("com.diffplug.gradle.spotless")
    id("net.corda.plugins.cordapp")
    id("net.corda.plugins.quasar-utils")
}

cordapp {
    val platformVersion = (rootProject.extra["platformVersion"] as String).toInt()
    targetPlatformVersion = platformVersion
    minimumPlatformVersion = platformVersion
    workflow {
        name = "Zk Notary App"
        vendor = "ING Bank NV"
        licence = "Apache License, Version 2.0"
        versionId = 1
    }
}

sourceSets {
    main {
        resources {
            srcDir(rootProject.file("config/dev"))
        }
    }
    test {
        resources {
            srcDir(rootProject.file("config/test"))
        }
    }
}

dependencies {
    val kotlinVersion = rootProject.extra["kotlinVersion"]
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("junit:junit:${rootProject.extra["junitVersion"]}")
    testImplementation("org.mockito:mockito-core:2.+")

    // Corda dependencies.
    val cordaReleaseGroup = rootProject.extra["cordaReleaseGroup"]
    val cordaVersion = rootProject.extra["cordaVersion"]
    cordaCompile("$cordaReleaseGroup:corda-core:$cordaVersion")
    cordaRuntime("$cordaReleaseGroup:corda:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-node:$cordaVersion")
    cordaCompile("$cordaReleaseGroup:corda-jackson:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-node-driver:$cordaVersion")
    testImplementation("$cordaReleaseGroup:corda-test-utils:$cordaVersion")

    // Other
    implementation(group = "net.java.dev.jna", name = "jna", version = "5.3.1")
}

spotless {
    kotlin {
        ktlint("0.37.1")
    }
}

