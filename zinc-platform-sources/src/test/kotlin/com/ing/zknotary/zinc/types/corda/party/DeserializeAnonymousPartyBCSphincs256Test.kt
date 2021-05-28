package com.ing.zknotary.zinc.types.corda.party

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.testing.core.TestIdentity
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeAnonymousPartyBCSphincs256Test :
    DeserializationTestBase<DeserializeAnonymousPartyBCSphincs256Test, DeserializeAnonymousPartyBCSphincs256Test.Data>({
        it.data.toZincJson(BCSphincs256Surrogate.ENCODED_SIZE)
    }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAnonymousPartyBCSphincs256Test>()

    @Serializable
    data class Data(val data: @Polymorphic AbstractParty)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.SPHINCS256_SHA256).party.anonymise()),
        )
    }
}
