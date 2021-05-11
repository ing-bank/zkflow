package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.identity.AnonymousParty
import java.security.PublicKey

object AnonymousPartySerializer : KSerializer<AnonymousParty> by (
    SurrogateSerializer(AnonymousPartySurrogate.serializer()) {
        AnonymousPartySurrogate(it.owningKey)
    }
    )

@Serializable
data class AnonymousPartySurrogate(val owningKey: PublicKey) : Surrogate<AnonymousParty> {
    override fun toOriginal() = AnonymousParty(owningKey)
}
