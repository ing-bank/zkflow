package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.Surrogate
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
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

    @Serializable
    data class ContainsListOfPublicKey2(
        @Serializable(with = List_0::class)
        val list: List<PublicKey>
    ) {
        object List_0 : FixedLengthListSerializer<PublicKey>(5, List_1)

        object List_1 : SerializerWithDefault<PublicKey>(
            RSAKey512Serializer,
            KeyPairGenerator.getInstance("RSA").run {
                initialize(512)
                genKeyPair().public as RSAPublicKeyImpl
            }
        )
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
        }

        object EncodedSerializer : FixedLengthByteArraySerializer(94)
    }

    object RSAKey512Serializer : SurrogateSerializer<PublicKey, RSAKeySurrogate>(
        RSAKeySurrogate.serializer(), { RSAKeySurrogate.from(it) }
    )

    // @Disabled("enable when list serialization error reporting will be fixed")
    // @Test
    // fun `Class with lists of the different PublicKeys must fail`() {
    //     val rsaKeyGen = KeyPairGenerator
    //         .getInstance("RSA")
    //         .also { it.initialize(512) }
    //     val rsaPK = rsaKeyGen.genKeyPair().public
    //     val dsaKeyGen = KeyPairGenerator
    //         .getInstance("DSA")
    //         .also { it.initialize(512) }
    //     val dsaPK = dsaKeyGen.genKeyPair().public
    //
    //     val keys = listOf(dsaPK, rsaPK, dsaPK)
    //
    //     val e = assertThrows<IllegalArgumentException> {
    //         engine.assertRoundTrip(ContainsListOfPublicKey2(keys), ContainsListOfPublicKey2.serializer())
    //     }
    //     println(e.message)
    // }

    // @Test
    // fun `structures must be parsable`() {
    //     walk(ContainsListOfPublicKey2.serializer().descriptor)
    //     assert(true)
    // }
}
