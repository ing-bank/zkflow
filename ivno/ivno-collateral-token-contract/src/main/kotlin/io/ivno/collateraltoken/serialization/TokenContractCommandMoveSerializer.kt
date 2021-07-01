package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers.CLASS_NAME_SIZE
import com.ing.zknotary.common.serialization.bfl.serializers.getOriginalClass
import com.ing.zknotary.common.serialization.bfl.serializers.toBytes
import io.dasl.contracts.v1.token.TokenContract
import kotlinx.serialization.Serializable
import net.corda.core.contracts.Contract

object TokenContractCommandMoveSerializer: SurrogateSerializer<TokenContract.Command.Move, TokenContractCommandMoveSurrogate>(
    TokenContractCommandMoveSurrogate.serializer(),
    { TokenContractCommandMoveSurrogate(it.contract?.toBytes()) }
)

@Serializable
@Suppress("ArrayInDataClass")
data class TokenContractCommandMoveSurrogate(
    @FixedLength([CLASS_NAME_SIZE])
    val contract_class_name: ByteArray?
): Surrogate<TokenContract.Command.Move> {
    override fun toOriginal() = TokenContract.Command.Move(contract_class_name?.let {
        @Suppress("UNCHECKED_CAST")
        contract_class_name.getOriginalClass() as? Class<out Contract>}
    )
}
