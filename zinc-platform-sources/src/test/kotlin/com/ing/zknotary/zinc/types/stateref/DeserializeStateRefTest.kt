package com.ing.zknotary.zinc.types.stateref

import com.ing.zknotary.common.crypto.ZINC
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeStateRefTest :
    DeserializationTestBase<DeserializeStateRefTest, DeserializeStateRefTest.Data>({ it.data.toZincJson() }) {
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
