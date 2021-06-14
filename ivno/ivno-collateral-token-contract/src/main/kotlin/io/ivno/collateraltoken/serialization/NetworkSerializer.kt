package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.identity.AbstractParty

@Serializable
data class NetworkSurrogate(
    @FixedLength([VALUE_LENGTH])
    val value: String,
    val operator: @Polymorphic AbstractParty?,
) : Surrogate<Network> {
    override fun toOriginal() = Network(value, operator)

    companion object {
        const val VALUE_LENGTH = 20
    }
}

object NetworkSerializer : SurrogateSerializer<Network, NetworkSurrogate>(
    NetworkSurrogate.serializer(),
    { NetworkSurrogate(it.value, it.operator) }
)