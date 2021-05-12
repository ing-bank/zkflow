package com.ing.zknotary.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import net.corda.core.crypto.Crypto
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializePartyBCECK1Test :
    DeserializationTestBase<DeserializePartyBCECK1Test, DeserializePartyBCECK1Test.Data>({
        it.data.toZincJson(
            serialName = BCECSurrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCECSurrogate.ENCODED_SIZE,
        )
    }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializePartyBCECK1Test>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.ECDSA_SECP256K1_SHA256)
    }

    @Serializable
    data class Data(val data: @Contextual Party)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.ECDSA_SECP256K1_SHA256).party),
        )
    }
}
