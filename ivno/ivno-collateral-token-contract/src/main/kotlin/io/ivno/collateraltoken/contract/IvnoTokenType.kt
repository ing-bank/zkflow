package io.ivno.collateraltoken.contract

import io.dasl.contracts.v1.token.TokenTypeState
import io.dasl.contracts.v1.token.linearPointer
import io.onixlabs.corda.bnms.contract.Network
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * Represents a token type definition.
 * @property network The network that the token type belongs to.
 * @property custodian The entity responsible for maintaining the underlying collateral fund.
 * @property tokenIssuingEntity The entity responsible for maintaining the token type.
 * @property displayName The display name of the token type.
 * @property fractionDigits Determines the number of fractional digits for the specified currency.
 * @property linearId A [UniqueIdentifier] used to track transition of this state.
 */
@BelongsToContract(IvnoTokenTypeContract::class)
data class IvnoTokenType(
    val network: Network,
    override val custodian: Party,
    val tokenIssuingEntity: Party,
    val displayName: String,
    val fractionDigits: Int = 0,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : QueryableState, TokenTypeState(
    symbol = displayName,
    exponent = fractionDigits,
    description = "",
    issuer = tokenIssuingEntity
) {

    override val participants: List<AbstractParty>
        get() = setOf(issuer, custodian).toList()

    fun toPointer(): LinearPointer<IvnoTokenType> {
        return this.linearPointer()
    }

    /**
     * Maps this state to a persistent state.
     */
    override fun generateMappedObject(schema: MappedSchema): PersistentState = when (schema) {
        is IvnoTokenTypeSchema.IvnoTokenTypeSchemaV1 -> IvnoTokenTypeSchema.IvnoTokenTypeEntity(
            linearId = linearId.id,
            externalId = linearId.externalId,
            networkName = network.value,
            networkOperator = network.operator,
            tokenIssuingEntity = tokenIssuingEntity,
            custodian = custodian,
            displayName = displayName,
            fractionDigits = fractionDigits
        )
        else -> throw IllegalArgumentException("Unrecognised schema: $schema.")
    }

    /**
     * Gets a list of supported state schemas.
     */
    override fun supportedSchemas(): List<MappedSchema> = listOf(IvnoTokenTypeSchema.IvnoTokenTypeSchemaV1)
}
