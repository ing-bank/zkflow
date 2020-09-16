package com.ing.zknotary.common.testing

import net.corda.core.crypto.SignatureScheme
import net.corda.core.crypto.toStringShort
import net.corda.core.internal.unspecifiedCountry
import net.corda.testing.core.TestIdentity

fun TestIdentity.Companion.fixed(
    organisation: String,
    signatureScheme: SignatureScheme = net.corda.core.crypto.Crypto.DEFAULT_SIGNATURE_SCHEME
): TestIdentity {

    val fixedKeyPair = com.ing.zknotary.common.zkp.ZKNulls.fixedKeyPair(signatureScheme)
    return TestIdentity(
        net.corda.core.identity.CordaX500Name(
            organisation,
            fixedKeyPair.public.toStringShort(),
            net.corda.core.identity.CordaX500Name.unspecifiedCountry
        ),
        fixedKeyPair
    )
}
