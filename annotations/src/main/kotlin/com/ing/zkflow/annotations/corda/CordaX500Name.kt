@file:Suppress("MatchingDeclarationName")
package com.ing.zkflow.annotations.corda

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import net.corda.core.identity.CordaX500Name
import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class CordaX500NameSpec<S : Surrogate<CordaX500Name>>(
    val provider: KClass<out ConversionProvider<CordaX500Name, out Surrogate<CordaX500Name>>>
)
