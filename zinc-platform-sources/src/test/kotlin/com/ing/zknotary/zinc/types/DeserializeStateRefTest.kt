package com.ing.zknotary.zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
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
            Data(StateRef(SecureHash.hashAs(SecureHash.SHA2_512, "Hello World!".toByteArray()), 2))
        )
    }
}
