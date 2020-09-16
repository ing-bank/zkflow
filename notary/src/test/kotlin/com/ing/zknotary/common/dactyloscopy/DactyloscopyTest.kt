package com.ing.zknotary.common.dactyloscopy

import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.sha256
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.unspecifiedCountry
import net.corda.core.utilities.OpaqueBytes
import org.junit.Test
import java.security.PublicKey
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class DactyloscopyTest {
    private val dactyloscopist = Dactyloscopist()

    @Test
    fun `int must be fingerprintable`() {
        val fingerprint = dactyloscopist.identify(1)
        assert(fingerprint.contentEquals(ByteArray(4) { if (it < 3) { 0 } else { 1 } }))
    }

    @Test
    fun `Byte array must short circuit`() {
        val array = "ZKP".toByteArray()
        val fingerprint = dactyloscopist.identify(array)
        assert(fingerprint.contentEquals(array))
    }

    @Test
    fun `Public key must be fingerprintable`() {
        val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
        val fingerprint = dactyloscopist.identify(fixedKeyPair.public)
        assert(fingerprint.contentEquals(fixedKeyPair.public.encoded))
    }

    @Test
    fun `AbstractParty must be fingerprintable`() {
        val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
        val party = Party(
            CordaX500Name("alice", fixedKeyPair.public.toStringShort(), CordaX500Name.unspecifiedCountry),
            fixedKeyPair.public
        )
        val fingerprint = dactyloscopist.identify(party)
        assert(fingerprint.contentEquals(fixedKeyPair.public.encoded))
    }

    @Test
    fun `SecureHash must be fingerprintable`() {
        val hash = ByteArray(1) { 0 }.sha256()
        val fingerprint = dactyloscopist.identify(hash)
        assert(fingerprint.contentEquals(hash.bytes))
    }

    @Test
    fun `Fails on class with no pub fields`() {
        val obj = object {
            private val a = 0
            private val b = 1
        }
        assertFails("Non decomposable type must fail") {
            dactyloscopist.identify(obj)
        }
    }

    @Test
    fun `Compound types must be fingerprintable`() {
        val obj = object {
            val a = 0
            private val b = 2
        }
        assert(dactyloscopist.identify(obj).contentEquals(ByteArray(4) { 0 }))
    }

    @Test
    fun `Multiple fingerprintable interface are not allowed`() {
        val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)

        val obj = object : PublicKey, AbstractParty(fixedKeyPair.public) {
            // PublicKey members.
            override fun getAlgorithm(): String {
                TODO("Not yet implemented")
            }
            override fun getFormat(): String {
                TODO("Not yet implemented")
            }
            override fun getEncoded() = ByteArray(1) { 0 }

            // AbstractParty members.
            override fun nameOrNull(): CordaX500Name? {
                TODO("Not yet implemented")
            }
            override fun ref(bytes: OpaqueBytes): PartyAndReference {
                TODO("Not yet implemented")
            }
        }

        assertFailsWith<MultipleFingerprintImplementations>("Types implementing multiple fingerprintables must fail") {
            dactyloscopist.identify(obj)
        }
    }

    @Test
    fun `Multiple fingerprintable interface overriden by direct Fingerprintable implementation`() {
        val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)

        val obj = object : Fingerprintable, PublicKey, AbstractParty(fixedKeyPair.public) {
            // Fingerprintable members.
            override fun fingerprint() = ByteArray(1) { 0 }

            // PublicKey members.
            override fun getAlgorithm(): String {
                TODO("Not yet implemented")
            }
            override fun getFormat(): String {
                TODO("Not yet implemented")
            }
            override fun getEncoded() = ByteArray(1) { 0 }

            // AbstractParty members.
            override fun nameOrNull(): CordaX500Name? {
                TODO("Not yet implemented")
            }
            override fun ref(bytes: OpaqueBytes): PartyAndReference {
                TODO("Not yet implemented")
            }
        }

        assert(dactyloscopist.identify(obj).contentEquals(ByteArray(1) { 0 }))
    }
}
