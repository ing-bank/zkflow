package com.ing.zknotary.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.AnonymisingPartyAndReferenceSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.AnonymousPartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
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

class DeserializeAnonymousPartyBCECK1AndReferenceTest :
    DeserializationTestBase<DeserializeAnonymousPartyBCECK1AndReferenceTest, DeserializeAnonymousPartyBCECK1AndReferenceTest.Data>({
        it.data.toZincJson(
            anonymous = true,
            serialName = BCECSurrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCECSurrogate.ENCODED_SIZE
        )
    }) {
    override fun getZincZKService() = getZincZKService<DeserializeAnonymousPartyBCECK1AndReferenceTest>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.ECDSA_SECP256K1_SHA256) +
            SerializersModule {
                contextual(AnonymisingPartyAndReferenceSerializer)
            }
    }

    @Serializable
    data class Data(val data: @Contextual PartyAndReference)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            val reference = OpaqueBytes(Random(42).nextBytes(AnonymousPartyAndReferenceSurrogate.REFERENCE_SIZE))
            return listOf(
                Data(
                    PartyAndReference(
                        TestIdentity.fresh("Alice", Crypto.ECDSA_SECP256K1_SHA256).party,
                        reference
                    )
                ),
                Data(
                    PartyAndReference(
                        TestIdentity.fresh("Alice", Crypto.ECDSA_SECP256K1_SHA256).party.anonymise(),
                        reference
                    )
                ),
            )
        }
    }
}
