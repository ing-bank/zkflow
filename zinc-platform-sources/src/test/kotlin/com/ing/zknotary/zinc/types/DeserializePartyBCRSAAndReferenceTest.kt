package com.ing.zknotary.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.core.TestIdentity
import kotlin.random.Random
import kotlin.reflect.full.findAnnotation

class DeserializePartyBCRSAAndReferenceTest :
    DeserializationTestBase<DeserializePartyBCRSAAndReferenceTest, DeserializePartyBCRSAAndReferenceTest.Data>({
        it.data.toZincJson(
            anonymous = false,
            serialName = BCRSASurrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCRSASurrogate.ENCODED_SIZE
        )
    }) {
    override fun getZincZKService() = getZincZKService<DeserializePartyBCRSAAndReferenceTest>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.RSA_SHA256) +
            SerializersModule {
                contextual(PartyAndReferenceSerializer)
            }
    }

    @Serializable
    data class Data(val data: @Contextual PartyAndReference)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            val reference = OpaqueBytes(Random(42).nextBytes(PartyAndReferenceSurrogate.REFERENCE_SIZE))
            return listOf(
                Data(
                    PartyAndReference(
                        TestIdentity.fresh("Alice", Crypto.RSA_SHA256).party,
                        reference
                    )
                ),
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
