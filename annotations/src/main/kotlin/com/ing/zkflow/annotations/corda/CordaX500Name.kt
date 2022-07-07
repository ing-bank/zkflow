@file:Suppress("MatchingDeclarationName")
package com.ing.zkflow.annotations.corda

import com.ing.zkflow.Surrogate
import net.corda.core.identity.CordaX500Name

@Target(AnnotationTarget.TYPE)
annotation class CordaX500NameSpec<S : Surrogate<CordaX500Name>>
