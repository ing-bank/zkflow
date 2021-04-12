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
            val serializersModule = CordaSignatureSchemeToSerializers.serializersModuleFor(it)

            val pk1 = Crypto.generateKeyPair(it).public
            val pk2 = Crypto.generateKeyPair(it).public

            roundTrip(pk1, serializersModule)
            sameSize(pk1, pk2, serializersModule)
        }
    }

    @Test
    fun `serialize and deserialize Corda-supported PublicKey include in a class`() {
        Crypto.supportedSignatureSchemes().filter {
            it.schemeNumberID != Crypto.COMPOSITE_KEY.schemeNumberID
        }.forEach {
            val serializersModule = CordaSignatureSchemeToSerializers.serializersModuleFor(it)

            val data1 = Data(Crypto.generateKeyPair(it).public)
            val data2 = Data(Crypto.generateKeyPair(it).public)

            roundTrip(data1, serializersModule)
            sameSize(data1, data2, serializersModule)
        }
    }
}
