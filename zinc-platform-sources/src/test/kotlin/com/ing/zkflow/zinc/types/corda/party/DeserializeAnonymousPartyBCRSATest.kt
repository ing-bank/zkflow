package com.ing.zkflow.zinc.types.corda.party

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.testing.core.TestIdentity
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeAnonymousPartyBCRSATest :
    DeserializationTestBase<DeserializeAnonymousPartyBCRSATest, DeserializeAnonymousPartyBCRSATest.Data>({
        it.data.toZincJson(BCRSASurrogate.ENCODED_SIZE)
    }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAnonymousPartyBCRSATest>()

    @Serializable
    data class Data(val data: @Polymorphic AbstractParty)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.RSA_SHA256).party.anonymise()),
        )
    }
}
