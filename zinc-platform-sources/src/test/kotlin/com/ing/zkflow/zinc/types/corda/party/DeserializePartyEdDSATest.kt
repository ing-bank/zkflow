package com.ing.zkflow.zinc.types.corda.party

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.testing.core.TestIdentity

class DeserializePartyEdDSATest : DeserializationTestBase <DeserializePartyEdDSATest, DeserializePartyEdDSATest.Data>({
    it.data.toZincJson(EdDSASurrogate.ENCODED_SIZE)
}) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializePartyEdDSATest>()

    @Serializable
    data class Data(val data: @Polymorphic AbstractParty)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.EDDSA_ED25519_SHA512).party),
        )
    }
}
