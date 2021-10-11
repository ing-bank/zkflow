package io.ivno.collateraltoken.serialization

import com.ing.zkflow.testing.assertRoundTripSucceeds
import com.ing.zkflow.testing.assertSameSize
import io.dasl.contracts.v1.token.TokenTransactionSummary.NettedAccountAmount
import io.ivno.collateraltoken.zinc.types.anotherNettedAccountAmount
import io.ivno.collateraltoken.zinc.types.nettedAccountAmount
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class TokenTransactionSummaryNettedAccountAmountSerializerTest {
    @Serializable
    data class Data(val value: @Contextual NettedAccountAmount)

    private val serializersModule = IvnoSerializers.serializersModule

    @Test
    fun `serialize and deserialize TokenTransactionSummary$NettedAccountAmount directly`() {
        assertRoundTripSucceeds(nettedAccountAmount, serializersModule)
        assertSameSize(nettedAccountAmount, anotherNettedAccountAmount, serializersModule)
    }

    @Test
    fun `serialize and deserialize TokenTransactionSummary$NettedAccountAmount`() {
        val data1 = Data(nettedAccountAmount)
        val data2 = Data(anotherNettedAccountAmount)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}