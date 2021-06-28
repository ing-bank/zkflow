package io.ivno.collateraltoken.serialization

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.dasl.contracts.v1.token.TokenTransactionSummary.State
import io.ivno.collateraltoken.zinc.types.anotherNettedAccountAmount
import io.ivno.collateraltoken.zinc.types.anotherParty
import io.ivno.collateraltoken.zinc.types.nettedAccountAmount
import io.ivno.collateraltoken.zinc.types.party
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test
import java.time.Instant

class TokenTransactionSummaryStateSerializerTest {
    @Serializable
    data class Data(val value: @Contextual State)

    private val serializersModule = IvnoSerializers.serializersModule

    @Test
    fun `serialize and deserialize TokenTransactionSummary$State directly`() {
        assertRoundTripSucceeds(state, serializersModule)
        assertSameSize(state, anotherState, serializersModule)
    }

    @Test
    fun `serialize and deserialize TokenTransactionSummary$State`() {
        val data1 = Data(state)
        val data2 = Data(anotherState)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }

    companion object {
        val state = State(
            listOf(party),
            "A command",
            listOf(nettedAccountAmount),
            "A description",
            Instant.now(),
        )
        val anotherState = State(
            listOf(party, anotherParty),
            "Another command",
            listOf(nettedAccountAmount, anotherNettedAccountAmount),
            "Another description",
            Instant.now().plusSeconds(42),
            SecureHash.zeroHash
        )
    }
}
