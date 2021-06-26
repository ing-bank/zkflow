package io.ivno.collateraltoken.serialization

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import io.dasl.contracts.v1.account.AccountAddress
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenTransactionSummary.NettedAccountAmount
import io.ivno.collateraltoken.zinc.types.anotherTokenDescriptor
import io.ivno.collateraltoken.zinc.types.tokenDescriptor
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name
import org.junit.jupiter.api.Test

class TokenTransactionSummaryNettedAccountAmountSerializerTest {
    @Serializable
    data class Data(val value: @Contextual NettedAccountAmount)

    private val someString = "Prince"
    private val anotherString = "Tafkap"
    private val someCordaX500Name = CordaX500Name.parse("O=tafkap,L=New York,C=US")
    private val anotherCordaX500Name = CordaX500Name.parse("O=prince,L=New York,C=US")

    private val nettedAccountAmount1 = NettedAccountAmount(
        AccountAddress(someString, someCordaX500Name),
        BigDecimalAmount(1, tokenDescriptor)
    )
    private val nettedAccountAmount2 = NettedAccountAmount(
        AccountAddress(anotherString, anotherCordaX500Name),
        BigDecimalAmount(42, anotherTokenDescriptor)
    )

    private val serializersModule = IvnoSerializers.serializersModule

    @Test
    fun `serialize and deserialize TokenTransactionSummary$NettedAccountAmount directly`() {
        assertRoundTripSucceeds(nettedAccountAmount1, serializersModule)
        assertSameSize(nettedAccountAmount1, nettedAccountAmount2, serializersModule)
    }

    @Test
    fun `serialize and deserialize TokenTransactionSummary$NettedAccountAmount`() {
        val data1 = Data(nettedAccountAmount1)
        val data2 = Data(nettedAccountAmount2)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}