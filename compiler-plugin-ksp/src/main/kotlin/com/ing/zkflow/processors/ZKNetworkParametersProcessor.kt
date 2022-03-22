package com.ing.zkflow.processors

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.ksp.implementations.ImplementationsProcessor
import com.ing.zkflow.ksp.implementations.ScopedDeclaration
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import kotlin.reflect.KClass

class ZKNetworkParametersProcessor : ImplementationsProcessor<ZKNetworkParameters> {
    override val interfaceClass = ZKNetworkParameters::class
    override val additionalInterfaces: Set<KClass<Any>> = emptySet()

    override fun process(implementations: List<ScopedDeclaration>): ServiceLoaderRegistration =
        ServiceLoaderRegistration(interfaceClass, implementations.map { it.java.qualifiedName })
}
