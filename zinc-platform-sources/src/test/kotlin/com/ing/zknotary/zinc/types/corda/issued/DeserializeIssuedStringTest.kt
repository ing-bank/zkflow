package com.ing.zknotary.zinc.types.corda.issued

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.serialization.bfl.corda.IssuedSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.core.TestIdentity
import kotlin.random.Random

class DeserializeIssuedStringTest : DeserializationTestBase<DeserializeIssuedStringTest, DeserializeIssuedStringTest.Data>({
    val toZincJson = it.data.toZincJson(encodedSize = EdDSASurrogate.ENCODED_SIZE)
    println(toZincJson)
    toZincJson
}) {
    override fun getZincZKService() = getZincZKService<DeserializeIssuedStringTest>()
    override fun getSerializersModule(): SerializersModule {
        return SerializersModule {
            contextual(IssuedSerializer(String.serializer()))
        }
    }
    @Serializable
    data class Data(@FixedLength([1]) val data: @Contextual Issued<String>)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            val reference = OpaqueBytes(Random(42).nextBytes(PartyAndReferenceSurrogate.REFERENCE_SIZE))
            val alice = TestIdentity.fresh("Alice", Crypto.EDDSA_ED25519_SHA512).party.anonymise()
            return listOf(Data(Issued(PartyAndReference(alice, reference), "X")))
        }
    }
}
