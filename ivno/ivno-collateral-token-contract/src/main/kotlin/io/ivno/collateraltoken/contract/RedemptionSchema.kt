package io.ivno.collateraltoken.contract

import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import javax.persistence.*

object RedemptionSchema {

    object RedemptionSchemaV1 : MappedSchema(
        schemaFamily = RedemptionSchema.javaClass,
        version = 1,
        mappedTypes = listOf(RedemptionEntity::class.java)
    ) {
        override val migrationResource = "redemption-schema.changelog-master"
    }

    @Entity
    @Table(name = "redemption_states")
    class RedemptionEntity(
        @Column(name = "linear_id", nullable = false)
        val linearId: UUID = UUID.randomUUID(),

        @Column(name = "external_id", nullable = true)
        val externalId: String? = null,

        @Column(name = "redeemer", nullable = false)
        val redeemer: AbstractParty = NullKeys.NULL_PARTY,

        @Column(name = "custodian", nullable = false)
        val custodian: AbstractParty = NullKeys.NULL_PARTY,

        @Column(name = "amount", nullable = false)
        val amount: BigDecimal = BigDecimal.ZERO,

        @Column(name = "token_type_linear_id", nullable = false)
        val tokenTypeLinearId: UUID = UUID.randomUUID(),

        @Column(name = "token_type_external_id", nullable = true)
        val tokenTypeExternalId: String? = null,

        @Column(name = "status", nullable = false)
        @Enumerated(EnumType.STRING)
        val status: RedemptionStatus = RedemptionStatus.REQUESTED,

        @Column(name = "timestamp", nullable = false)
        val timestamp: Instant = Instant.MIN,

        @Column(name = "account_id", nullable = false)
        val accountId: String = ""

    ) : PersistentState()
}
