package com.ing.zkflow.common.network

import java.util.ServiceLoader

object ZKNetworkParametersServiceLoader {
    private val orderedParametersList: Map<Int, ZKNetworkParameters> by lazy {
        val parametersList = ArrayList<ZKNetworkParameters>()
        ServiceLoader.load(ZKNetworkParameters::class.java).forEach { parametersList.add(it) }
        parametersList.sortedBy { it.version }.associateBy { it.version }
    }

    private val latestVersion: Int by lazy {
        orderedParametersList.keys.maxOrNull() ?: error(
            "At least one ZKNetworkParameters should be present on the classpath, found none."
        )
    }

    val latest: ZKNetworkParameters = orderedParametersList.getValue(latestVersion)

    fun getVersion(version: Int): ZKNetworkParameters? = orderedParametersList[version]
}
