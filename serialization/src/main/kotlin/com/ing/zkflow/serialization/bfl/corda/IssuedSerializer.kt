package com.ing.zkflow.serialization.bfl.corda

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference

@Serializable
data class IssuedSurrogate<T : Any>(
    val issuer: @Contextual PartyAndReference,
    val product: @Contextual T
) : Surrogate<Issued<T>> {
    override fun toOriginal(): Issued<T> = Issued(issuer, product)
}

class IssuedSerializer<T : Any>(productSerializer: KSerializer<T>) :
    SurrogateSerializer<Issued<T>, IssuedSurrogate<T>>(
        IssuedSurrogate.serializer(productSerializer),
        { IssuedSurrogate(it.issuer, it.product) }
    )
