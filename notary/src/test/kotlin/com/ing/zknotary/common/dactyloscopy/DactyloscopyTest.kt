package com.ing.zknotary.common.dactyloscopy

import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.sha256
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.unspecifiedCountry
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.OpaqueBytes
import org.junit.Test
import java.security.PublicKey
import java.util.Random
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class DactyloscopyTest {
    @Test
    fun `int must be fingerprintable`() {
        val fingerprint = Dactyloscopist.identify(1)
        assert(fingerprint.contentEquals(ByteArray(4) { if (it < 3) { 0 } else { 1 } }))
    }

    @Test
    fun `Byte array must short circuit`() {
        val array = "ZKP".toByteArray()
        val fingerprint = Dactyloscopist.identify(array)
        assert(fingerprint.contentEquals(array))
    }

    @Test
    fun `Public key must be fingerprintable`() {
        val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
        val fingerprint = Dactyloscopist.identify(fixedKeyPair.public)
        assert(fingerprint.contentEquals(fixedKeyPair.public.encoded))
    }

    @Test
    fun `AbstractParty must be fingerprintable`() {
        val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
        val party = Party(
            CordaX500Name("alice", fixedKeyPair.public.toStringShort(), CordaX500Name.unspecifiedCountry),
            fixedKeyPair.public
        )
        val fingerprint = Dactyloscopist.identify(party)
        assert(fingerprint.contentEquals(fixedKeyPair.public.encoded))
    }

    @Test
    fun `SecureHash must be fingerprintable`() {
        val hash = ByteArray(1) { 0 }.sha256()
        val fingerprint = Dactyloscopist.identify(hash)
        assert(fingerprint.contentEquals(hash.bytes))
    }

    @Test
    fun `Fails on class with no pub fields`() {
        val obj = object {
            private val a = 0
            private val b = 1
        }
        assertFails("Non decomposable type must fail") {
            Dactyloscopist.identify(obj)
        }
    }

    @Test
    fun `Compound types must be fingerprintable`() {
        val obj = object {
            val a = 0
            private val b = 2
        }
        assert(Dactyloscopist.identify(obj).contentEquals(ByteArray(4) { 0 }))
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
            Dactyloscopist.identify(obj)
        }
    }

    @Test
    fun `Fingerprint a list`() {
        val list = listOf(2, 1)
        val fingerprint = ByteArray(0) + listOf<Byte>(0, 0, 0, 2, 0, 0, 0, 1)

        assert(Dactyloscopist.identify(list).contentEquals(fingerprint))
    }

    @Test
    fun `Skip non fingerprintable fields`() {
        data class Table(val feet: Int = 0, @NonFingerprintable("Test") val top: Int = 1)

        val table = Table()
        assert(Dactyloscopist.identify(table).contentEquals(table.feet.fingerprint()))
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

        assert(Dactyloscopist.identify(obj).contentEquals(ByteArray(1) { 0 }))
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

        assert(Dactyloscopist.identify(state).contentEquals(fingerprint))
    }

    @Test
    fun `Fingerprint a command`() {
        val command = TestContract.Create()
        val fingerprint = command.id.fingerprint()

        assert(Dactyloscopist.identify(command).contentEquals(fingerprint))
    }

    // ================================ Service definitions ===========================================
    // This definition is a copy of ZKCommandData _without_ implementing the Fingerprintable interface.
    // Copying it here serves the testing purposes.
    interface ZKCommandData : CommandData {
        val id: Int
        val paddingConfiguration: ComponentPaddingConfiguration
    }

    // This definition is a copy of TestContract _without_ implementing the Fingerprintable interface.
    class TestContract : Contract {
        companion object {
            const val PROGRAM_ID: ContractClassName = "com.ing.zknotary.common.contracts.TestContract"
        }

        data class TestState(
            override val owner: AbstractParty,
            val value: Int = Random().nextInt(1000)
        ) : ContractState, OwnableState {

            @NonFingerprintable("Temporary removed from fingerprinting")
            override val participants = listOf(owner)

            override fun withNewOwner(newOwner: AbstractParty) = CommandAndState(Move(), copy(owner = newOwner))
        }

        // Commands
        class Create : ZKCommandData {
            override val id: Int = 0
            override val paddingConfiguration = ComponentPaddingConfiguration.Builder().empty()
        }

        class Move : ZKCommandData {
            override val id: Int = 1
            override val paddingConfiguration = ComponentPaddingConfiguration.Builder().empty()
        }

        override fun verify(tx: LedgerTransaction) {}
    }
}
