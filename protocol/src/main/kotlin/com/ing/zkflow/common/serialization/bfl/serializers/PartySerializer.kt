package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

object PartySerializer :
    SurrogateSerializer<Party, PartySurrogate>(PartySurrogate.serializer(), { PartySurrogate(it.name, it.owningKey) })

@Serializable
@SerialName("l")
data class PartySurrogate(
    val name: @Contextual CordaX500Name,
    val owningKey: PublicKey
) : Surrogate<Party> {
    override fun toOriginal() = Party(name, owningKey)
}
