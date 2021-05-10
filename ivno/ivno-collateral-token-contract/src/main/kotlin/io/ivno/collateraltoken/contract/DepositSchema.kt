package io.ivno.collateraltoken.contract

import net.corda.core.crypto.NullKeys.NULL_PARTY
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import javax.persistence.*

object DepositSchema {

    object DepositSchemaV1 : MappedSchema(
        schemaFamily = DepositSchema.javaClass,
        version = 1,
        mappedTypes = listOf(DepositEntity::class.java)
    ) {
        override val migrationResource = "deposit-schema.changelog-master"
    }

    @Entity
    @Table(name = "deposit_states")
    class DepositEntity(
        @Column(name = "linear_id", nullable = false)
        val linearId: UUID = UUID.randomUUID(),

        @Column(name = "external_id", nullable = true)
        val externalId: String? = null,

        @Column(name = "depositor", nullable = false)
        val depositor: AbstractParty = NULL_PARTY,

        @Column(name = "custodian", nullable = false)
        val custodian: AbstractParty = NULL_PARTY,

        @Column(name = "token_issuing_entity", nullable = false)
        val tokenIssuingEntity: AbstractParty = NULL_PARTY,

        @Column(name = "amount", nullable = false)
        val amount: BigDecimal = BigDecimal.ZERO,

        @Column(name = "token_type_linear_id", nullable = false)
        val tokenTypeLinearId: UUID = UUID.randomUUID(),

        @Column(name = "token_type_external_id", nullable = true)
        val tokenTypeExternalId: String? = null,

        @Column(name = "reference", nullable = true)
        val reference: String? = null,

        @Column(name = "status", nullable = false)
        @Enumerated(EnumType.STRING)
        val status: DepositStatus = DepositStatus.DEPOSIT_REQUESTED,

        @Column(name = "timestamp", nullable = false)
        val timestamp: Instant = Instant.MIN,

        @Column(name = "account_id", nullable = false)
        val accountId: String = ""

    ) : PersistentState()
}
