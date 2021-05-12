package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

// TODO: In future versions of BFL, only a single implementation of AbstractParty will be allowed
//  in collections (List, Map, etc). Under such condition, AbstractParty will be serialized using
//  either AnonymousPartySurrogate or PartySurrogate, rendering AbstractPartySurrogate obsolete.

object AbstractPartySerializer : KSerializer<AbstractParty> by (
    SurrogateSerializer(AbstractPartySurrogate.serializer()) {
        when (it) {
            is AnonymousParty -> AbstractPartySurrogate(null, it.owningKey)
            is Party -> AbstractPartySurrogate(it.name, it.owningKey)
            else -> error("AbstractParty is only known to be implemented by Party and AnonymousParty")
        }
    }
    )

@Serializable
data class AbstractPartySurrogate(
    val name: @Serializable(with = CordaX500NameSerializer::class) CordaX500Name?,
    val owningKey: PublicKey
) : Surrogate<AbstractParty> {
    override fun toOriginal() = if (name == null) {
        AnonymousParty(owningKey)
    } else {
        Party(name, owningKey)
    }
}
