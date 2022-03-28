package io.ivno.annotated.fixtures

import com.ing.zkflow.AnonymousParty_EdDSA_DefaultProvider
import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Default
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.annotations.corda.EdDSA
import io.ivno.annotated.deps.Network
import net.corda.core.identity.AnonymousParty

@ZKPSurrogate(NetworkAnonymousOperatorConverter::class)
data class NetworkEdDSAAnonymousOperator(
    val value: @Size(10) String,
    val operator: @EdDSA @Default<AnonymousParty>(AnonymousParty_EdDSA_DefaultProvider::class) AnonymousParty?
) : Surrogate<Network> {
    override fun toOriginal() = Network(value, operator)
}

object NetworkAnonymousOperatorConverter : ConversionProvider<Network, NetworkEdDSAAnonymousOperator> {
    override fun from(original: Network): NetworkEdDSAAnonymousOperator {
        val operator = if (original.operator != null) {
            require(original.operator is AnonymousParty) { "Network must be managed by an anonymous party" }
            original.operator
        } else {
            null
        }

        return NetworkEdDSAAnonymousOperator(original.value, operator)
    }
}
