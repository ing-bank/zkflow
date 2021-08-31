package com.ing.zknotary.common.zkp

import io.kotest.assertions.throwables.shouldThrow
import net.corda.core.crypto.SignatureScheme
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.junit.jupiter.api.Test

class ZKFlowTest {
    @Test
    fun `Unsupported signature scheme fails`() {
        val bogusSignatureScheme = SignatureScheme(
            99999,
            "BOGUS",
            AlgorithmIdentifier(ASN1ObjectIdentifier("1.9.999")),
            emptyList(),
            "BOGUS_SECURITYPROVIDER",
            "BOGUS_ALGORITHM_NAME",
            "BOGUS_SIGNATURE_NAME",
            null,
            null,
            "Bogus signature scheme for testing"
        )

        shouldThrow<IllegalArgumentException> {
            ZKFlow.requireSupportedSignatureScheme(bogusSignatureScheme)
        }
    }
}
