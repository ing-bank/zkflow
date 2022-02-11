package com.ing.zkflow.processors

import com.ing.zkflow.common.network.ZKNetworkParametersProvider
import com.ing.zkflow.ksp.implementations.ImplementationsProcessor
import com.ing.zkflow.ksp.implementations.ScopedDeclaration
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration

class ZKNetworkParametersProviderProcessor : ImplementationsProcessor<ZKNetworkParametersProvider> {
    override val interfaceClass = ZKNetworkParametersProvider::class

    override fun process(implementations: List<ScopedDeclaration>): ServiceLoaderRegistration =
        if (implementations.size <= 1) {
            ServiceLoaderRegistration(interfaceClass, implementations.map { it.java.qualifiedName })
        } else {
            error("There should be max one implementation of ZKNetworkParametersProvider on the classpath. Found ${implementations.size}")
        }
}
