package com.ing.zknotary.testing.fixtures.state

import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.time.ZonedDateTime
import java.util.Currency

@CordaSerializable
public data class Deal(
    val amount: Amount<Currency>,
    val weight: Weight,

    val proposer: AbstractParty,
    val accepter: AbstractParty,

    val expiryDate: ZonedDateTime,

    val location: Location,
    val locationName: LocationName,

    val attachments: List<SecureHash>,

    val period: Int?,
    val proposerReference: String? = null,
    val additionalApprovers: List<AbstractParty>? = null
)

public typealias LocationName = String

@CordaSerializable
public enum class WeightUnit {
    KG,
    LBS
}

@CordaSerializable
public data class Weight(
    val quantity: Double,
    val unit: WeightUnit
)

@CordaSerializable
public data class Location(
    val country: String,
    val state: String?,
    val city: String
)

@CordaSerializable
public enum class Confirmation {
    YES,
    NO,
}
