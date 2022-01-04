@file:Suppress("MatchingDeclarationName")
package com.ing.zkflow.annotations.corda

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import net.corda.core.identity.CordaX500Name
import kotlin.reflect.KClass

annotation class CordaX500NameSpec(
    val provider: KClass<out ConversionProvider<CordaX500Name, out Surrogate<CordaX500Name>>>
)
