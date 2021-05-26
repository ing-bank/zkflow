package io.ivno.collateraltoken.contract

import net.corda.core.crypto.NullKeys.NULL_PARTY
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import javax.persistence.*

object TransferSchema {

    object TransferSchemaV1 : MappedSchema(
        schemaFamily = TransferSchema.javaClass,
        version = 1,
        mappedTypes = listOf(TransferEntity::class.java)
    ) {
        override val migrationResource = "transfer-schema.changelog-master"
    }

    @Entity
    @Table(name = "transfer_states")
    class TransferEntity(
        @Column(name = "linear_id", nullable = false)
        val linearId: UUID = UUID.randomUUID(),

        @Column(name = "external_id", nullable = true)
        val externalId: String? = null,

        @Column(name = "current_token_holder", nullable = false)
        val currentTokenHolder: AbstractParty = NULL_PARTY,

        @Column(name = "target_token_holder", nullable = false)
        val targetTokenHolder: AbstractParty = NULL_PARTY,

        @Column(name = "initiator", nullable = false)
        @Enumerated(EnumType.STRING)
        val initiator: TransferInitiator = TransferInitiator.CURRENT_HOLDER,

        @Column(name = "amount", nullable = false)
        val amount: BigDecimal = BigDecimal.ZERO,

        @Column(name = "token_type_linear_id", nullable = false)
        val tokenTypeLinearId: UUID = UUID.randomUUID(),

        @Column(name = "token_type_external_id", nullable = true)
        val tokenTypeExternalId: String? = null,

        @Column(name = "status", nullable = false)
        @Enumerated(EnumType.STRING)
        val status: TransferStatus = TransferStatus.REQUESTED,

        @Column(name = "timestamp", nullable = false)
        val timestamp: Instant = Instant.MIN,

        @Column(name = "current_token_holder_account_id", nullable = false)
        val currentTokenHolderAccountId: String = "",

        @Column(name = "target_token_holder_account_id", nullable = false)
        val targetTokenHolderAccountId: String = ""

    ) : PersistentState()
}
