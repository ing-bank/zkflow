package com.ing.zknotary.zinc.types.corda.issued

import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.zinc.types.generateDifferentValueThan
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.ExperimentalTime

@ExperimentalTime
class IssuedStringEqualsTest {
    private val zincZKService = getZincZKService<IssuedStringEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(aliceA, aliceA, true)
    }

    @Test
    fun `Issued with different products should not be equal`() {
        performEqualityTest(aliceA, aliceB, false)
    }

    @Test
    fun `Issued with different identities should not be equal`() {
        performEqualityTest(aliceA, bobA, false)
    }

    @Test
    fun `Issued with different references should not be equal`() {
        performEqualityTest(aliceA, aliceOtherReferenceA, false)
    }

    private fun performEqualityTest(
        left: Issued<String>,
        right: Issued<String>,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(encodedSize = EdDSASurrogate.ENCODED_SIZE)
            )
            put(
                "right",
                right.toJsonObject(encodedSize = EdDSASurrogate.ENCODED_SIZE)
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        private val random = Random(42)
        private val reference = OpaqueBytes(random.nextBytes(PartyAndReferenceSurrogate.REFERENCE_SIZE))
        private val anotherReference = generateDifferentValueThan(reference) {
            OpaqueBytes(random.nextBytes(PartyAndReferenceSurrogate.REFERENCE_SIZE))
        }
        private val alice = TestIdentity.fresh("Alice", Crypto.EDDSA_ED25519_SHA512).party.anonymise()
        private val bob = TestIdentity.fresh("Bob", Crypto.EDDSA_ED25519_SHA512).party.anonymise()
        private val aliceA = Issued(PartyAndReference(alice, reference), "A")
        private val aliceB = Issued(PartyAndReference(alice, reference), "B")
        private val aliceOtherReferenceA = Issued(PartyAndReference(alice, anotherReference), "A")
        private val bobA = Issued(PartyAndReference(bob, reference), "A")
    }
}
