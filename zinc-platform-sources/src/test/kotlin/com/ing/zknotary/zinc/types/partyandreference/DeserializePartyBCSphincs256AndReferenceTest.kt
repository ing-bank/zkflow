package com.ing.zknotary.zinc.types.partyandreference

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
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

class DeserializePartyBCSphincs256AndReferenceTest :
    DeserializationTestBase<DeserializePartyBCSphincs256AndReferenceTest, DeserializePartyBCSphincs256AndReferenceTest.Data>({
        it.data.toZincJson(
            anonymous = false,
            serialName = BCSphincs256Surrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCSphincs256Surrogate.ENCODED_SIZE
        )
    }) {
    override fun getZincZKService() = getZincZKService<DeserializePartyBCSphincs256AndReferenceTest>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.SPHINCS256_SHA256) +
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
                        TestIdentity.fresh("Alice", Crypto.SPHINCS256_SHA256).party,
                        reference
                    )
                ),
                Data(
                    PartyAndReference(
                        TestIdentity.fresh("Alice", Crypto.SPHINCS256_SHA256).party.anonymise(),
                        reference
                    )
                ),
            )
        }
    }
}
