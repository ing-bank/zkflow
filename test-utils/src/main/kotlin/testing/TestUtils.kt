package testing

import net.corda.core.crypto.Crypto.DEFAULT_SIGNATURE_SCHEME
import net.corda.core.crypto.SignatureScheme
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.unspecifiedCountry
import net.corda.testing.core.TestIdentity

public fun TestIdentity.Companion.fixed(
    organisation: String,
    signatureScheme: SignatureScheme = DEFAULT_SIGNATURE_SCHEME
): TestIdentity {

    val fixedKeyPair = com.ing.zknotary.common.zkp.ZKNulls.fixedKeyPair(signatureScheme)
    return TestIdentity(
        CordaX500Name(
            organisation,
            fixedKeyPair.public.toStringShort(),
            CordaX500Name.unspecifiedCountry
        ),
        fixedKeyPair
    )
}
