package com.ing.zkflow.zinc.types.corda.partyandreference

import com.ing.zkflow.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zkflow.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.core.TestIdentity
import kotlin.random.Random

class DeserializePartyEdDSAAndReferenceTest : DeserializationTestBase <DeserializePartyEdDSAAndReferenceTest, DeserializePartyEdDSAAndReferenceTest.Data>({
    it.data.toZincJson(EdDSASurrogate.ENCODED_SIZE)
}) {
    override fun getZincZKService() = getZincZKService<DeserializePartyEdDSAAndReferenceTest>()

    @Serializable
    data class Data(val data: @Contextual PartyAndReference)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            val reference = OpaqueBytes(Random(42).nextBytes(PartyAndReferenceSurrogate.REFERENCE_SIZE))
            return listOf(
                Data(
                    PartyAndReference(
                        TestIdentity.fresh("Alice", Crypto.EDDSA_ED25519_SHA512).party,
                        reference
                    )
                ),
            )
        }
    }
}