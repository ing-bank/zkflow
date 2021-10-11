package io.ivno.collateraltoken.serialization

import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.contracts.UniqueIdentifier
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import java.util.UUID

class IvnoTokenTypeSerializerTest {
    @Serializable
    data class Data(val value: @Contextual IvnoTokenType)

    private val alice = TestIdentity.fresh("Alice").party
    private val bob = TestIdentity.fresh("Bob").party

    private val ivnoTokenType1 = IvnoTokenType(
        Network("Network 1", alice),
        bob,
        alice,
        "Display Name 1"
    )
    private val ivnoTokenType2 = IvnoTokenType(
        Network("Network 2", bob),
        alice,
        bob,
        "Display Name 2",
        1,
        UniqueIdentifier(id = UUID(0, 1))
    )

    private val serializersModule = IvnoSerializers.serializersModule

    @Test
    fun `serialize and deserialize IvnoTokenType directly`() {
        assertRoundTripSucceeds(ivnoTokenType1, serializersModule)
        assertSameSize(ivnoTokenType1, ivnoTokenType2, serializersModule)
    }

    @Test
    fun `serialize and deserialize IvnoTokenType`() {
        val data1 = Data(ivnoTokenType1)
        val data2 = Data(ivnoTokenType2)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}