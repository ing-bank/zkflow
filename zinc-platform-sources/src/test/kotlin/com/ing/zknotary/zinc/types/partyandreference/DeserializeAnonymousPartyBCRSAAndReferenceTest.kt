package com.ing.zknotary.zinc.types.partyandreference

import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.core.TestIdentity
import kotlin.random.Random
import kotlin.reflect.full.findAnnotation

class DeserializeAnonymousPartyBCRSAAndReferenceTest :
    DeserializationTestBase<DeserializeAnonymousPartyBCRSAAndReferenceTest, DeserializeAnonymousPartyBCRSAAndReferenceTest.Data>({
        it.data.toZincJson(
            serialName = BCRSASurrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCRSASurrogate.ENCODED_SIZE
        )
    }) {
    override fun getZincZKService() = getZincZKService<DeserializeAnonymousPartyBCRSAAndReferenceTest>()

    @Serializable
    data class Data(val data: @Contextual PartyAndReference)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            val reference = OpaqueBytes(Random(42).nextBytes(PartyAndReferenceSurrogate.REFERENCE_SIZE))
            return listOf(
                Data(
                    PartyAndReference(
                        TestIdentity.fresh("Alice", Crypto.RSA_SHA256).party.anonymise(),
                        reference
                    )
                ),
            )
        }
    }
}
