package com.ing.zkflow.zinc.types.corda.securehash

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.crypto.ZINC
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeSecureHashTest :
    DeserializationTestBase<DeserializeSecureHashTest, DeserializeSecureHashTest.Data>({ it.data.toZincJson() }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeSecureHashTest>()

    @Serializable
    data class Data(val data: @Contextual SecureHash)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(SecureHash.allOnesHash),
            Data(SecureHash.zeroHash),
            Data(SecureHash.hashAs(SecureHash.ZINC, "Hello World!".toByteArray()))
        )
    }
}
