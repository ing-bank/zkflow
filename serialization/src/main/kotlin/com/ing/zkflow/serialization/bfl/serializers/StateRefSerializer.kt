package com.ing.zkflow.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash

object StateRefSerializer : SurrogateSerializer<StateRef, StateRefSurrogate>(
    StateRefSurrogate.serializer(),
    { StateRefSurrogate(it.txhash, it.index) }
)

@Serializable
@SerialName("p")
data class StateRefSurrogate(
    val hash: @Contextual SecureHash,
    val index: Int
) : Surrogate<StateRef> {
    override fun toOriginal(): StateRef = StateRef(hash, index)
}