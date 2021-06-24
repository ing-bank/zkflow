package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.reified.debugSerialize
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.junit.jupiter.api.Test
import java.security.PublicKey

class PublicKeySerializerTest {
    @Serializable
    data class Data(val value: PublicKey)

    @Test
    fun `serializations of public keys must only include padding information`() {
        Crypto.supportedSignatureSchemes().filter {
            it.schemeNumberID != Crypto.COMPOSITE_KEY.schemeNumberID
        }.forEach {
            val pk = Crypto.generateKeyPair(it).public
            val ser = debugSerialize(pk, serializersModule = CordaSerializers.module)
            println("${it.schemeCodeName}:\n\tPublic key size = ${pk.encoded.size}\n\tserialized size = ${ser.first.size}")
            println(ser.second)

            val commonDiff = listOf(
                2, // length of the discriminator, encoded as a byte representation of a Short.
                2, // the discriminator; discriminator is 1 UTF-8 char, encoded as 2 bytes.
                4, // length of the `encoded` field, encoded as a byte representation of an Int.
            ).sum()

            val extraDiff = when (it.schemeNumberID) {
                // Crypto.ECDSA_SECP256K1_SHA256 and Crypto.ECDSA_SECP256R1_SHA256 correspond instances of
                // the same class constructed with different arguments.
                // Dependently on the argument, the payload is either 88 or 91 long.
                // All instances of the class use the same surrogate,
                // thus the relevant field in the surrogate is also 91 bytes long.
                Crypto.ECDSA_SECP256K1_SHA256.schemeNumberID -> listOf(
                    1, // byte length of a byte to distinguish the two.
                    3, // ECDSA_SECP256K1_SHA256 is 88 bytes long, but encoded with 91 bytes.
                ).sum()
                Crypto.ECDSA_SECP256R1_SHA256.schemeNumberID -> listOf(
                    1, // byte length of a byte to distinguish the two
                ).sum()
                else -> 0
            }

            (ser.first.size - pk.encoded.size) shouldBe (commonDiff + extraDiff)
        }
    }

    @Test
    fun `serialize and deserialize Corda-supported PublicKey directly`() {
        Crypto.supportedSignatureSchemes().filter {
            it.schemeNumberID != Crypto.COMPOSITE_KEY.schemeNumberID
        }.forEach {
            val pk1 = Crypto.generateKeyPair(it).public
            val pk2 = Crypto.generateKeyPair(it).public

            assertRoundTripSucceeds(pk1)
            assertSameSize(pk1, pk2)
        }
    }

    @Test
    fun `serialize and deserialize Corda-supported PublicKey include in a class`() {
        Crypto.supportedSignatureSchemes().filter {
            it.schemeNumberID != Crypto.COMPOSITE_KEY.schemeNumberID
        }.forEach {
            val data1 = Data(Crypto.generateKeyPair(it).public)
            val data2 = Data(Crypto.generateKeyPair(it).public)

            assertRoundTripSucceeds(data1)
            assertSameSize(data1, data2)
        }
    }
}
