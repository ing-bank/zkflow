package com.ing.zkflow.ksp.implementations

import kotlin.reflect.KClass

/**
 * A processor that processes implementations of [interfaceClasses].
 */
interface ImplementationsProcessor<T : Any> {
    val interfaceClass: KClass<T>
    val additionalInterfaces: Set<KClass<Any>>
    val allInterfaces: Set<KClass<out Any>>
        get() = additionalInterfaces + interfaceClass

    fun process(implementations: List<ScopedDeclaration>): ServiceLoaderRegistration
}

/**
 *  Of this provider class there will be these implementations.
 */
data class ServiceLoaderRegistration(val providerClass: KClass<*>, val implementations: List<String>)
