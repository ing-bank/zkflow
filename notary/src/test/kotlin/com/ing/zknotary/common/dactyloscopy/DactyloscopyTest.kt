package com.ing.zknotary.common.dactyloscopy

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.zkp.ZKNulls
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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
import java.nio.ByteBuffer
import java.security.PublicKey

class DactyloscopyTest {
    @Test
    fun `int must be fingerprintable`() {
        val fingerprint = Dactyloscopist.identify(1)

        fingerprint shouldBe ByteArray(4) { if (it < 3) { 0 } else { 1 } }
    }

    @Test
    fun `Byte array must short circuit`() {
        val array = "ZKP".toByteArray()
        val fingerprint = Dactyloscopist.identify(array)

        fingerprint shouldBe array
    }

    @Test
    fun `Public key must be fingerprintable`() {
        val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
        val fingerprint = Dactyloscopist.identify(fixedKeyPair.public)

        fingerprint shouldBe fixedKeyPair.public.encoded
    }

    @Test
    fun `AbstractParty must be fingerprintable`() {
        val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
        val party = Party(
            CordaX500Name("alice", fixedKeyPair.public.toStringShort(), CordaX500Name.unspecifiedCountry),
            fixedKeyPair.public
        )
        val fingerprint = Dactyloscopist.identify(party)

        fingerprint shouldBe fixedKeyPair.public.encoded
    }

    @Test
    fun `SecureHash must be fingerprintable`() {
        val hash = ByteArray(1) { 0 }.sha256()
        val fingerprint = Dactyloscopist.identify(hash)

        fingerprint shouldBe hash.bytes
    }

    @Test
    fun `Fails on class with no pub fields`() {
        val obj = object {
            private val a = 0
            private val b = 1
        }

        val exception = shouldThrow<MustHavePublicMembers> {
            Dactyloscopist.identify(obj)
        }
        val errorMessage =
            "Type with no associated fingerprinting functionality must have public members: ${obj::class.qualifiedName}"

        exception.message shouldBe errorMessage
    }

    @Test
    fun `Compound types must be fingerprintable`() {
        val obj = object {
            val a = 0
            private val b = 2
        }

        Dactyloscopist.identify(obj) shouldBe ByteArray(4) { 0 }
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

        shouldThrow<MultipleFingerprintImplementations> {
            Dactyloscopist.identify(obj)
        }
    }

    @Test
    fun `Fingerprint a list`() {
        val list = listOf(2, 1)
        val fingerprint = ByteArray(0) + listOf<Byte>(0, 0, 0, 2, 0, 0, 0, 1)

        Dactyloscopist.identify(list) shouldBe fingerprint
    }

    @Test
    fun `Skip non fingerprintable fields`() {
        data class Table(val feet: Int = 0, @NonFingerprintable("Test") val top: Int = 1)

        val table = Table()

        Dactyloscopist.identify(table) shouldBe table.feet.fingerprint()
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

        Dactyloscopist.identify(obj) shouldBe ByteArray(1) { 0 }
    }

    @Test
    fun `Fingerprint a test state`() {
        val owner = ZKNulls.NULL_ANONYMOUS_PARTY
        val value = 1
        val state = TestContract.TestState(owner, value)
        val fingerprint =
            // owner
            owner.fingerprint() +
                //
                // participants = join of participants fingerprints
                // disabled for now
                // owner.fingerprint() +
                //
                // value
                value.fingerprint()

        Dactyloscopist.identify(state) shouldBe fingerprint
    }

    @Test
    fun `Fingerprint a string`() {
        val string = "YELLOW SUBMARINE"
        val fingerprint = string.toByteArray(Charsets.UTF_8)

        Dactyloscopist.identify(string) shouldBe fingerprint
    }

    @Test
    fun `Fingerprint a command`() {
        val command = TestContract.Create()
        val fingerprint = command.id.fingerprint()

        Dactyloscopist.identify(command) shouldBe fingerprint
    }
}
