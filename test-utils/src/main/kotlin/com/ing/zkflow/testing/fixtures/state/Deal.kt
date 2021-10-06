package com.ing.zkflow.testing.fixtures.state

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable
import java.time.ZonedDateTime
import java.util.Currency

@CordaSerializable
@Serializable
public data class Deal(
    val amount: @Contextual Amount<@Contextual Currency>,
    val weight: Weight,

    val proposer: @Contextual AnonymousParty,
    val accepter: @Contextual AnonymousParty,

    val expiryDate: @Contextual ZonedDateTime,

    val location: Location,
    val locationName: LocationName,

    val attachments: List<@Contextual SecureHash>,

    val period: Int?,
    val proposerReference: String? = null,
    @FixedLength([2]) val additionalApprovers: List<@Contextual AnonymousParty>? = null
)

public typealias LocationName = String

@CordaSerializable
@Serializable
public enum class WeightUnit {
    KG,
    LBS
}

@CordaSerializable
@Serializable
public data class Weight(
    val quantity: Double,
    val unit: WeightUnit
)

@CordaSerializable
@Serializable
public data class Location(
    @FixedLength([2]) val country: String,
    @FixedLength([100]) val state: String?,
    @FixedLength([100]) val city: String
)

@CordaSerializable
@Serializable
public enum class Confirmation {
    YES,
    NO,
}
