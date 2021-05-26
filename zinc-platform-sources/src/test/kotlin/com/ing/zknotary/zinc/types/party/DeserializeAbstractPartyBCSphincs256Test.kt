package com.ing.zknotary.zinc.types.party

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.testing.core.TestIdentity
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeAbstractPartyBCSphincs256Test :
    DeserializationTestBase<DeserializeAbstractPartyBCSphincs256Test, DeserializeAbstractPartyBCSphincs256Test.Data>({
        it.data.toZincJson(
            serialName = BCSphincs256Surrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCSphincs256Surrogate.ENCODED_SIZE,
        )
    }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAbstractPartyBCSphincs256Test>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.SPHINCS256_SHA256)
    }

    @Serializable
    data class Data(val data: @Contextual AbstractParty)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.SPHINCS256_SHA256).party),
            Data(TestIdentity.fresh("Alice", Crypto.SPHINCS256_SHA256).party.anonymise()),
        )
    }
}
