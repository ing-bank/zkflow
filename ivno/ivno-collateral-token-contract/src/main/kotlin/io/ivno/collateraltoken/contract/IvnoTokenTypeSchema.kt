package io.ivno.collateraltoken.contract

import net.corda.core.crypto.NullKeys.NULL_PARTY
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object IvnoTokenTypeSchema {

    object IvnoTokenTypeSchemaV1 : MappedSchema(
        schemaFamily = IvnoTokenTypeSchema.javaClass,
        version = 1,
        mappedTypes = listOf(IvnoTokenTypeEntity::class.java)
    ) {
        override val migrationResource = "ivno-token-type-schema.changelog-master"
    }

    @Entity
    @Table(name = "ivno_token_type_states")
    class IvnoTokenTypeEntity(
        @Column(name = "linear_id", nullable = false)
        val linearId: UUID = UUID.randomUUID(),

        @Column(name = "external_id", nullable = true)
        val externalId: String? = null,

        @Column(name = "network_name", nullable = false)
        val networkName: String = "",

        @Column(name = "network_operator", nullable = true)
        val networkOperator: AbstractParty? = null,

        @Column(name = "network_hash", nullable = false)
        val networkHash: String = "",

        @Column(name = "token_issuing_entity", nullable = false)
        val tokenIssuingEntity: AbstractParty = NULL_PARTY,

        @Column(name = "custodian", nullable = false)
        val custodian: AbstractParty = NULL_PARTY,

        @Column(name = "display_name", nullable = false)
        val displayName: String = "",

        @Column(name = "fraction_digits", nullable = false)
        val fractionDigits: Int = 0
    ) : PersistentState()
}
