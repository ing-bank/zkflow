package com.ing.zknotary.common.serialization.bfl.corda

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

@ExperimentalSerializationApi
object AbstractPartySerializer : KSerializer<AbstractParty> {
    private val strategy = AbstractPartySurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): AbstractParty {
        return decoder.decodeSerializableValue(strategy).toOriginal()
    }

    override fun serialize(encoder: Encoder, value: AbstractParty) {
        encoder.encodeSerializableValue(strategy, AbstractPartySurrogate.from(value))
    }
}

@ExperimentalSerializationApi
@Serializable
data class AbstractPartySurrogate(
    val name: @Serializable(with = CordaX500NameSerializer::class) CordaX500Name?,
    val owningKey: PublicKey
) {
    fun toOriginal() = if (name == null) {
        AnonymousParty(owningKey)
    } else {
        Party(name, owningKey)
    }

    companion object {
        fun from(original: AbstractParty) = with(original) {
            when (this) {
                is AnonymousParty -> AbstractPartySurrogate(null, owningKey)
                is Party -> AbstractPartySurrogate(name, owningKey)
                else -> error("AbstractParty is only known to be implemented by Party and AnonymousParty")
            }
        }
    }
}
