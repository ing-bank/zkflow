package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.serializers.BigDecimalSizes
import io.dasl.contracts.v1.account.AccountAddress
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenDescriptor
import io.dasl.contracts.v1.token.TokenTransactionSummary.NettedAccountAmount
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class TokenTransactionSummaryNettedAccountAmountSurrogate(
    val accountAddress: @Contextual AccountAddress,
    @Serializable(with = BigDecimalAmountSerializer::class)
    @BigDecimalSizes([AMOUNT_INT_LENGTH, AMOUNT_FRAC_LENGTH])
    val amount: BigDecimalAmount<@Contextual TokenDescriptor>,
) : Surrogate<NettedAccountAmount> {
    override fun toOriginal() = NettedAccountAmount(accountAddress, amount)

    companion object {
        const val AMOUNT_INT_LENGTH = 20
        const val AMOUNT_FRAC_LENGTH = 4
    }
}

object TokenTransactionSummaryNettedAccountAmountSerializer :
        SurrogateSerializer<NettedAccountAmount, TokenTransactionSummaryNettedAccountAmountSurrogate>(
            TokenTransactionSummaryNettedAccountAmountSurrogate.serializer(),
            { TokenTransactionSummaryNettedAccountAmountSurrogate(it.accountAddress, it.amount) }
        )