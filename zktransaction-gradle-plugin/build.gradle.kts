plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.github.gmazzo.buildconfig")
}

gradlePlugin {
    plugins {
        create("helloWorldPlugin") {
            id = "hello.world.hello-world-gradle-plugin"
            implementationClass = "hello.world.gradle.HelloWorldGradlePlugin"
        }
    }
}

val pluginName = "zkTransactionPlugin"

buildConfig {
    val group: String by project
    val version: String by project
    buildConfigField("String", "zktransactionPluginGroupId", "\"$group\"")
    buildConfigField("String", "zktransactionPluginArtifactId", "\"zktransaction-compiler-plugin\"")
    buildConfigField("String", "zktransactionPluginVersion", "\"$version\"")
    buildConfigField("String", "zktransactionPluginName", "\"$pluginName\"")
    packageName(group)
    useKotlinOutput {
        internalVisibility = true
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin-api"))
}
