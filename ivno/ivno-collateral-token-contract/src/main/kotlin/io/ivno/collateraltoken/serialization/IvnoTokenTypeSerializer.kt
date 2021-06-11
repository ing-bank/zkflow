package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@Serializable
data class IvnoTokenTypeSurrogate(
    val network: @Contextual Network,
    val custodian: @Contextual Party,
    val tokenIssuingEntity: @Contextual Party,
    @FixedLength([DISPLAY_NAME_LENGTH])
    val displayName: String,
    val fractionDigits: Int,
    val linearId: @Contextual UniqueIdentifier
) : Surrogate<IvnoTokenType> {
    override fun toOriginal() = IvnoTokenType(
        network,
        custodian,
        tokenIssuingEntity,
        displayName,
        fractionDigits,
        linearId
    )

    companion object {
        const val DISPLAY_NAME_LENGTH = 20

        fun from(ivnoTokenType: IvnoTokenType) = with(ivnoTokenType) {
            IvnoTokenTypeSurrogate(network, custodian, tokenIssuingEntity, displayName, fractionDigits, linearId)
        }
    }
}

object IvnoTokenTypeSerializer: SurrogateSerializer<IvnoTokenType, IvnoTokenTypeSurrogate>(
    IvnoTokenTypeSurrogate.serializer(),
    { IvnoTokenTypeSurrogate.from(it) }
)