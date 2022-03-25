@file:Suppress("ClassName")

package com.ing.zkflow

import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.ZKPSurrogate
import net.corda.core.identity.CordaX500Name

@ZKPSurrogate(CordaX500NameConverter::class)
data class CordaX500NameSurrogate(
    val concat: @ASCII(UPPER_BOUND) String
) : Surrogate<CordaX500Name> {
    override fun toOriginal(): CordaX500Name =
        CordaX500Name.parse(concat)

    companion object {
        const val UPPER_BOUND = 50
    }
}

object CordaX500NameConverter : ConversionProvider<CordaX500Name, CordaX500NameSurrogate> {
    override fun from(original: CordaX500Name): CordaX500NameSurrogate =
        CordaX500NameSurrogate("$original")
}

val fixedCordaX500Name = CordaX500Name.parse("O=BOGUS,L=New York,C=US")
