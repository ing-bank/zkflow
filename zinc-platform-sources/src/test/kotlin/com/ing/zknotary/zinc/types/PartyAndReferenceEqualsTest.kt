package com.ing.zknotary.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.AnonymousPartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.loggerFor
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class PartyAndReferenceEqualsTest {
    private val log = loggerFor<PartyAndReferenceEqualsTest>()
    private val zincZKService = getZincZKService<PartyAndReferenceEqualsTest>()

    companion object {
        private val reference by lazy {
            OpaqueBytes(Random(42).nextBytes(AnonymousPartyAndReferenceSurrogate.REFERENCE_SIZE))
        }
        private val anotherReference by lazy {
            OpaqueBytes(Random(AnonymousPartyAndReferenceSurrogate.REFERENCE_SIZE).nextBytes(AnonymousPartyAndReferenceSurrogate.REFERENCE_SIZE))
        }
        private val aliceIdentity = TestIdentity.fresh("Alice", Crypto.ECDSA_SECP256K1_SHA256)
        private val bobIdentity = TestIdentity.fresh("Bob", Crypto.ECDSA_SECP256K1_SHA256)
        val alice = PartyAndReference(aliceIdentity.party, reference)
        val aliceAnotherReference = PartyAndReference(aliceIdentity.party, anotherReference)
        val aliceAnonymous = PartyAndReference(aliceIdentity.party.anonymise(), reference)
        val bob = PartyAndReference(bobIdentity.party, reference)
    }

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
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
    fun `alice equals anonymous alice`() {
        performEqualityTest(alice, aliceAnonymous, true)
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
                    anonymous = false,
                    serialName = BCECSurrogate::class.findAnnotation<SerialName>()!!.value,
                    encodedSize = BCECSurrogate.ENCODED_SIZE
                )
            )
            put(
                "right",
                right.toJsonObject(
                    anonymous = false,
                    serialName = BCECSurrogate::class.findAnnotation<SerialName>()!!.value,
                    encodedSize = BCECSurrogate.ENCODED_SIZE
                )
            )
        }.toString()

        zincZKService.proveTimed(witness, log).let {
            zincZKService.verifyTimed(it, "$expected", log)
        }
    }

    @Serializable
    data class Data(val data: @Contextual PartyAndReference)
}
