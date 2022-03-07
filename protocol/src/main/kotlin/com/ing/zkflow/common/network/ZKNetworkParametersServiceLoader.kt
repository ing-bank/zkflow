package com.ing.zkflow.common.network

import java.util.ServiceLoader

object ZKNetworkParametersServiceLoader {
    private val orderedParametersList: Map<Int, ZKNetworkParameters> by lazy {
        val parametersList = ArrayList<ZKNetworkParameters>()
        ServiceLoader.load(ZKNetworkParameters::class.java).forEach { parametersList.add(it) }
        val sortedParameters = parametersList.sortedBy { it.version }
        check(sortedParameters.distinctBy { it.version }.size == sortedParameters.size) {
            val impls = sortedParameters.joinToString(", ") { "${it::class} (version ${it.version})" }
            "There are multiple implementations of ZKNetworkParameters on the classpath with the same version number. All implementations found: $impls"
        }
        sortedParameters.associateBy { it.version }
    }

    private val latestVersion: Int by lazy {
        orderedParametersList.keys.maxOrNull() ?: error(
            "At least one ZKNetworkParameters should be present on the classpath, found none."
        )
    }

    val latest: ZKNetworkParameters = orderedParametersList.getValue(latestVersion)

    fun getVersion(version: Int): ZKNetworkParameters? = orderedParametersList[version]
}
