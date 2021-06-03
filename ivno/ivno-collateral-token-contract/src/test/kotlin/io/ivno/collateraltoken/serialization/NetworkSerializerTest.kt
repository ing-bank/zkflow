package io.ivno.collateraltoken.serialization

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test


class NetworkSerializerTest {
    @Serializable
    data class Data(val value: @Contextual Network)

    private val network1 = Network(
        value = "Network 1",
        operator = TestIdentity.fresh("Alice").party.anonymise()
    )
    private val network2 = Network(
        value = "Network 2",
        operator = TestIdentity.fresh("Bob").party.anonymise()
    )

    private val serializersModule = SerializersModule { contextual(NetworkSerializer) }

    @Test
    fun `serialize and deserialize Network directly`() {
        assertRoundTripSucceeds(network1, serializersModule)
        assertSameSize(network1, network2, serializersModule)
    }

    @Test
    fun `serialize and deserialize Network`() {
        val data1 = Data(network1)
        val data2 = Data(network2)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}