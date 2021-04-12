package com.ing.zknotary.common.serialization

import com.ing.zknotary.common.serialization.bfl.corda.CordaSignatureSchemeToSerializers
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.junit.jupiter.api.Test
import java.security.PublicKey

class PublicKeySerializerTest {
    @Serializable
    data class Data(val value: PublicKey)

    @Test
    fun `serialize and deserialize Corda-supported PublicKey directly`() {
        Crypto.supportedSignatureSchemes().filter {
            it.schemeNumberID != Crypto.COMPOSITE_KEY.schemeNumberID
        }.forEach {
            val serializers = CordaSignatureSchemeToSerializers serializerFor it

            val pk1 = Crypto.generateKeyPair(it).public
            val pk2 = Crypto.generateKeyPair(it).public

            roundTrip(pk1, serializers)
            sameSize(pk1, pk2, serializers)
        }
    }

    @Test
    fun `serialize and deserialize Corda-supported PublicKey include in a class`() {
        Crypto.supportedSignatureSchemes().filter {
            it.schemeNumberID != Crypto.COMPOSITE_KEY.schemeNumberID
        }.forEach {
            val serializers = CordaSignatureSchemeToSerializers serializerFor it

            val data1 = Data(Crypto.generateKeyPair(it).public)
            val data2 = Data(Crypto.generateKeyPair(it).public)

            roundTrip(data1, serializers)
            sameSize(data1, data2, serializers)
        }
    }
}
