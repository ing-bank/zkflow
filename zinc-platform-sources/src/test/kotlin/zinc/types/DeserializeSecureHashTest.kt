package zinc.types

import com.ing.zknotary.common.zkp.ZincZKService
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
            Data(SecureHash.hashAs(SecureHash.Companion.SHA2_256, "Hello World!".toByteArray()))
        )
    }
}
