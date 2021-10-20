package com.ing.zkflow.zinc.types.corda.party

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.testing.core.TestIdentity

class DeserializePartyBCECK1Test : DeserializationTestBase <DeserializePartyBCECK1Test, DeserializePartyBCECK1Test.Data>({
    it.data.toZincJson(BCECSurrogate.ENCODED_SIZE)
}) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializePartyBCECK1Test>()

    @Serializable
    data class Data(val data: @Polymorphic AbstractParty)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.ECDSA_SECP256K1_SHA256).party),
        )
    }
}
