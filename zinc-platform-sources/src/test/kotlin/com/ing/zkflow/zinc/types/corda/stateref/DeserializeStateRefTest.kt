package com.ing.zkflow.zinc.types.corda.stateref

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.crypto.ZINC
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash

class DeserializeStateRefTest : DeserializationTestBase <DeserializeStateRefTest, DeserializeStateRefTest.Data>({ it.data.toZincJson() }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeStateRefTest>()

    @Serializable
    data class Data(val data: @Contextual StateRef)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(StateRef(SecureHash.allOnesHash, 0)),
            Data(StateRef(SecureHash.zeroHash, 1)),
            Data(StateRef(SecureHash.hashAs(SecureHash.ZINC, "Hello World!".toByteArray()), 2))
        )
    }
}
