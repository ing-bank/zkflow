package com.ing.zknotary.zinc.types.corda.partyandreference

import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.zinc.types.generateDifferentValueThan
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AnonymousPartyEdDSAAndReferenceEqualsTest {
    private val zincZKService = getZincZKService<AnonymousPartyEdDSAAndReferenceEqualsTest>()

    companion object {
        private val random by lazy { Random(42) }
        private val reference by lazy {
            OpaqueBytes(random.nextBytes(PartyAndReferenceSurrogate.REFERENCE_SIZE))
        }
        private val anotherReference by lazy {
            generateDifferentValueThan(reference) {
                OpaqueBytes(random.nextBytes(PartyAndReferenceSurrogate.REFERENCE_SIZE))
            }
        }
        private val aliceIdentity = TestIdentity.fresh("Alice", Crypto.EDDSA_ED25519_SHA512)
        private val bobIdentity = TestIdentity.fresh("Bob", Crypto.EDDSA_ED25519_SHA512)
        val alice = PartyAndReference(aliceIdentity.party.anonymise(), reference)
        val aliceAnotherReference = PartyAndReference(aliceIdentity.party.anonymise(), anotherReference)
        val bob = PartyAndReference(bobIdentity.party.anonymise(), reference)
    }

    @Test
    fun `alice equals alice`() {
        performEqualityTest(alice, alice, true)
    }

    @Test
    fun `alice does not equal alice with another reference`() {
        performEqualityTest(alice, aliceAnotherReference, false)
    }

    @Test
    fun `alice does not equals bob`() {
        performEqualityTest(alice, bob, false)
    }

    private fun performEqualityTest(
        left: PartyAndReference,
        right: PartyAndReference,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(
                    serialName = EdDSASurrogate::class.findAnnotation<SerialName>()!!.value,
                    encodedSize = EdDSASurrogate.ENCODED_SIZE
                )
            )
            put(
                "right",
                right.toJsonObject(
                    serialName = EdDSASurrogate::class.findAnnotation<SerialName>()!!.value,
                    encodedSize = EdDSASurrogate.ENCODED_SIZE
                )
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
