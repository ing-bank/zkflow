plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":protocol"))

    implementation(kotlin("stdlib"))

    val kspVersion: String by project
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")

    val kspTestingVersion: String by project
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:$kspTestingVersion")
}
