package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

object PartySerializer : KSerializer<Party> by (
    SurrogateSerializer(PartySurrogate.serializer()) {
        PartySurrogate(it.name, it.owningKey)
    }
    )

@Serializable
data class PartySurrogate(
    val name: @Contextual CordaX500Name,
    val owningKey: PublicKey
) : Surrogate<Party> {
    override fun toOriginal() = Party(name, owningKey)
}
