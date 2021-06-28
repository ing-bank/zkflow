package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.dasl.contracts.v1.token.TokenTransactionSummary.NettedAccountAmount
import io.dasl.contracts.v1.token.TokenTransactionSummary.State
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import java.time.Instant

@Serializable
data class TokenTransactionSummaryStateSurrogate(
    @FixedLength([PARTICIPANTS_LENGTH])
    val participants: List<@Polymorphic AbstractParty>,
    @FixedLength([COMMAND_LENGTH])
    val command: String,
    @FixedLength([AMOUNTS_LENGTH])
    val amounts: List<@Contextual NettedAccountAmount>,
    @FixedLength([DESCRIPTION_LENGTH])
    val description: String,
    val transactionTime: @Contextual Instant,
    val transactionId: @Contextual SecureHash?
) : Surrogate<State> {
    override fun toOriginal() = State(participants, command, amounts, description, transactionTime, transactionId)

    companion object {
        const val PARTICIPANTS_LENGTH = 2
        const val COMMAND_LENGTH = 20
        const val AMOUNTS_LENGTH = 3
        const val DESCRIPTION_LENGTH = 20

        fun from(state: State) = with(state) {
            TokenTransactionSummaryStateSurrogate(participants, command, amounts, description, transactionTime, transactionId)
        }
    }
}

object TokenTransactionSummaryStateSerializer : SurrogateSerializer<State, TokenTransactionSummaryStateSurrogate>(
    TokenTransactionSummaryStateSurrogate.serializer(),
    { TokenTransactionSummaryStateSurrogate.from(it) }
)