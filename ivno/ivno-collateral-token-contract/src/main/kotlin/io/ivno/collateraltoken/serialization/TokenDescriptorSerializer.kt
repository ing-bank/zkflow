package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.dasl.contracts.v1.token.TokenDescriptor
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name

object TokenDescriptorSerializer : SurrogateSerializer<TokenDescriptor, TokenDescriptorSurrogate> (
TokenDescriptorSurrogate.serializer(),
    { TokenDescriptorSurrogate(it.symbol, it.issuerName) })

@Serializable
data class TokenDescriptorSurrogate(
    @FixedLength([SYMBOL_SIZE])
    val symbol: String,
    val issuerName: @Contextual CordaX500Name
) : Surrogate<TokenDescriptor> {
    override fun toOriginal() = TokenDescriptor(symbol, issuerName)

    companion object {
        // TODO what is a reasonable value for this?
        const val SYMBOL_SIZE = 32
    }
}
