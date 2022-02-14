package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.BFLEngine
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import sun.security.rsa.RSAPublicKeyImpl
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class SurrogateSerializerTest : SerializerTest {
    private fun getRsaPubKey() = KeyPairGenerator.getInstance("RSA").run {
        initialize(512)
        genKeyPair().public
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `PublicKey must serialize via a surrogate`(engine: SerdeEngine) {
        engine.assertRoundTrip(RSAKey512Serializer, getRsaPubKey())
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `PublicKeys must have equal length serialization`(engine: SerdeEngine) {
        engine.serialize(RSAKey512Serializer, getRsaPubKey()).size shouldBe
            engine.serialize(RSAKey512Serializer, getRsaPubKey()).size
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Class with lists of the same PublicKeys must be (de)serializable`(engine: SerdeEngine) {
        engine.assertRoundTrip(ContainsListOfPublicKey2.serializer(), ContainsListOfPublicKey2(List(2) { getRsaPubKey() }))
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `Sizes for PublicKeys must match`(engine: BFLEngine) {
        val expectedRsaKeyEncodedBytesSize = Int.SIZE_BYTES + RSAKeySurrogate.RSA_ENCODED_SIZE
        RSAKey512Serializer.descriptor.byteSize shouldBe expectedRsaKeyEncodedBytesSize
        engine.serialize(RSAKey512Serializer, getRsaPubKey()).size shouldBe expectedRsaKeyEncodedBytesSize * engine.bytesScaler

        val expectedListRsaKeysBytesSize = Int.SIZE_BYTES + ContainsListOfPublicKey2.MAX_KEYS * expectedRsaKeyEncodedBytesSize
        ContainsListOfPublicKey2.serializer().descriptor.toFixedLengthSerialDescriptorOrThrow().byteSize shouldBe expectedListRsaKeysBytesSize
        engine.serialize(ContainsListOfPublicKey2.serializer(), ContainsListOfPublicKey2(List(2) { getRsaPubKey() })).size shouldBe expectedListRsaKeysBytesSize * engine.bytesScaler
    }

    @Serializable
    data class ContainsListOfPublicKey2(
        @Serializable(with = List_0::class)
        val list: List<PublicKey>
    ) {
        object List_0 : FixedLengthListSerializer<PublicKey>(MAX_KEYS, List_1)

        object List_1 : SerializerWithDefault<PublicKey>(
            RSAKey512Serializer,
            KeyPairGenerator.getInstance("RSA").run {
                initialize(512)
                genKeyPair().public as RSAPublicKeyImpl
            }
        )

        companion object {
            const val MAX_KEYS = 5
        }
    }

    @Suppress("ArrayInDataClass")
    @Serializable
    data class RSAKeySurrogate(
        @Serializable(with = EncodedSerializer::class)
        val encoded: ByteArray,
    ) : Surrogate<PublicKey> {
        override fun toOriginal(): RSAPublicKeyImpl = KeyFactory
            .getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKeyImpl

        companion object {
            fun from(pk: PublicKey) = RSAKeySurrogate(pk.encoded)
            const val RSA_ENCODED_SIZE = 94
        }

        object EncodedSerializer : FixedLengthByteArraySerializer(RSA_ENCODED_SIZE)
    }

    object RSAKey512Serializer : SurrogateSerializer<PublicKey, RSAKeySurrogate>(
        RSAKeySurrogate.serializer(), { RSAKeySurrogate.from(it) }
    )
}
