mapOf(
    "spotlessPluginVersion" to "3.28.1",
    "cordaReleaseGroup" to "net.corda",
    "cordaCoreReleaseGroup" to "net.corda",
    "cordaVersion" to "4.5.6-ING",
    "gradlePluginsVersion" to "5.0.4",
    "kotlinVersion" to "1.3.72",
    "junitVersion" to "4.12",
    "quasarVersion" to "0.7.10",
    "log4jVersion" to "2.11.2",
    "platformVersion" to "5",
    "slf4jVersion" to "1.7.25",
    "nettyVersion" to "4.1.22.Final"
).entries.forEach {
    project.extra.set(it.key, it.value)
}

