package com.example.contract.audit

import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.versioning.Versioned
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import java.time.Instant

class AuditContract : Contract {
    companion object {
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }

    interface VersionedAuditRecord : ContractState, Versioned

    @ZKP
    data class AuditRecord(
        val auditor: @EdDSA Party,
        val auditInfo: @ASCII(AUDIT_INFO_LENGTH) String,
        val auditInstant: Instant = Instant.now(),
    ) : VersionedAuditRecord {
        override val participants: List<AbstractParty> = listOf(auditor)

        companion object {
            const val AUDIT_INFO_LENGTH = 30
        }
    }

    override fun verify(tx: LedgerTransaction) {
        val auditRecords = tx.outputsOfType<AuditRecord>()

        requireThat {
            "There is a single audit record" using (auditRecords.size == 1)
            "There is a timewindow" using (tx.timeWindow != null)
            "The audit record fits in the timewindow" using (auditRecords.first().auditInstant in tx.timeWindow!!)
        }
    }
}