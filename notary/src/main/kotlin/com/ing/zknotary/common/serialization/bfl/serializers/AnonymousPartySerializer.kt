package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey

object AnonymousPartySerializer :
    SurrogateSerializer<AnonymousParty, AnonymousPartySurrogate>(
        AnonymousPartySurrogate.serializer(),
        { AnonymousPartySurrogate(it.owningKey) }
    )

@Serializable
@SerialName("k")
data class AnonymousPartySurrogate(val owningKey: PublicKey) : Surrogate<AnonymousParty> {
    override fun toOriginal() = AnonymousParty(owningKey)
}
