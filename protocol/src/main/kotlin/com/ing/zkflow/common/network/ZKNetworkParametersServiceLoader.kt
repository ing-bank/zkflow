package com.ing.zkflow.common.network

import java.util.ServiceLoader

object ZKNetworkParametersServiceLoader {
    val parameters: ZKNetworkParameters by lazy {
        val parametersList = ArrayList<ZKNetworkParametersProvider>()
        ServiceLoader.load(ZKNetworkParametersProvider::class.java).forEach { parametersList.add(it) }
        require(parametersList.size == 1) { "Exactly one ZKNetworkParametersProvider should be registered, found ${parametersList.size}" }
        parametersList.single().parameters
    }
}
