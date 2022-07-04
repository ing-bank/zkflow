plugins {
    kotlin("jvm")
    id("net.corda.plugins.quasar-utils")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version "1.5.31-1.0.0"
}

// This will prevent conflicts for between the original artifacts and their tests.
// group = "$group.integration"

repositories {
    maven("https://software.r3.com/artifactory/corda")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    testImplementation(project(":test-utils"))
    testImplementation(project(":protocol"))
    testImplementation(project(":zinc-poet:zinc-code-generation"))

    val cordaVersion: String by project
    kotlinCompilerPluginClasspath("net.corda:corda-core:$cordaVersion")
    kotlinCompilerPluginClasspath(project(":utils"))
    kotlinCompilerPluginClasspath(project(":annotations"))
    kotlinCompilerPluginClasspath(project(":serialization"))

    val arrowMetaVersion: String by project
    kotlinCompilerPluginClasspath("io.arrow-kt:arrow-meta:$arrowMetaVersion")

    implementation(project(":compiler-plugin-ksp"))
    ksp(project(":compiler-plugin-ksp"))
}

// project.tasks.getByPath("compileKotlin").finalizedBy("generateZincCircuits")
// project.tasks.getByPath("compileTestKotlin").finalizedBy("generateZincCircuits")
// project.tasks.getByPath("assemble").dependsOn("generateZincCircuits")
// task("generateZincCircuits") {
//     doLast {
//         project.javaexec {
//             val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
//             val test = javaPlugin.sourceSets.findByName("test") ?: error("Can't find main sourceSet")
//             main = "com.ing.zkflow.zinc.poet.generate.GenerateZincCircuitsKt"
//
//             // We must add 'build/generated/ksp/src/main/resources' to the main sourceSet if it exists, because
//             // otherwise the generated META-INF/services file is not picked up by the `generateZincCircuits` task.
//             // It would be the nicest if KSP already did this, however it doesn't.
//             classpath = test.runtimeClasspath + project.files(project.buildDir.resolve("generated/ksp/test/resources")).filter(File::exists)
//         }
//     }
// }

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Generated class files are not recreated upon changes in compiler-plugin-arrow, therefor we always clean the
    // build, to enforce rebuild of classes with the updated compiler plugin.
    dependsOn += "clean"
    dependsOn += ":compiler-plugin-arrow:jar"

    kotlinOptions {
        // IR backend is needed for Unsigned integer types support for kotlin 1.4, in $rootDir/build.gradle.kts:185 we
        // explicitly enforce 1.4.
        useIR = true
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xplugin=$rootDir/compiler-plugin-arrow/build/libs/compiler-plugin-arrow-$version.jar"
        freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    }
}
