plugins {
    kotlin("jvm")
    id("com.ing.zknotary.gradle-plugin") version "0.1.4-SNAPSHOT"
//    id("com.ing.zknotary.gradle.plugins.zargo") version "0.1-SNAPSHOT"
//    id("com.ing.zknotary.gradle.plugins.znc") version "0.1-SNAPSHOT"
}

// zinc {
//    zargoCommandPath = File("/Users/EU88FH/Developer/corda-zkp/zinc/target/release/zargo")
//    zncCommandPath = File("/Users/EU88FH/Developer/corda-zkp/zinc/target/release/znc")
//    zncVerbosity = 2
// }
//
// sourceSets {
//    main {
//        zinc {
//            setSrcDirs(mutableListOf(project.buildDir))
//            exclude("**/test.zn")
//        }
//    }
// }

group = "com.ing.zknotary"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

// tasks.withType(com.ing.zknotary.gradle.zinc.tasks.ZncCompile::class) {
//    dependsOn("copyZinc")
// }
