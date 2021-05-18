package com.ing.zknotary.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.AnonymisingPartyAndReferenceSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.AnonymousPartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
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

class DeserializeAnonymousPartyEdDSAAndReferenceTest :
    DeserializationTestBase<DeserializeAnonymousPartyEdDSAAndReferenceTest, DeserializeAnonymousPartyEdDSAAndReferenceTest.Data>({
        it.data.toZincJson(
            anonymous = true,
            serialName = EdDSASurrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = EdDSASurrogate.ENCODED_SIZE
        )
    }) {
    override fun getZincZKService() = getZincZKService<DeserializeAnonymousPartyEdDSAAndReferenceTest>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.EDDSA_ED25519_SHA512) +
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
                        TestIdentity.fresh("Alice", Crypto.EDDSA_ED25519_SHA512).party,
                        reference
                    )
                ),
                Data(
                    PartyAndReference(
                        TestIdentity.fresh("Alice", Crypto.EDDSA_ED25519_SHA512).party.anonymise(),
                        reference
                    )
                ),
            )
        }
    }
}
