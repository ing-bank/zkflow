package com.ing.zknotary.common.zkp

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.contracts.ZKOwnableState
import com.ing.zknotary.testing.fixed
import com.ing.zknotary.testing.fixtures.contract.TestContract
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Random
import kotlin.time.ExperimentalTime

/**
 * MockAuditContract is a third party contract.
 * This means we can't annotate it, nor change its contents.
 */
class MockAuditContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zknotary.common.zkp.MockAuditContract"
    }

    @BelongsToContract(MockAuditContract::class)
    data class Approval(
        val approver: AnonymousParty
    ) : ContractState {
        @FixedLength([1])
        override val participants: List<@Contextual AnonymousParty> = listOf(approver)
    }

    /**
     * Audit records are not private, and therefore have no associated circuit.
     * If this command is used in a ZKFlow transaction, ZKFlow will still require
     * command metadata, so it can determine total component group/witness size.
     */
    class Approve : TypeOnlyCommandData()

    override fun verify(tx: LedgerTransaction) = TODO("Not yet implemented")
}

val MockAuditContract.Approval.metadata: ZKCommandMetadata
    get() = commandMetadata {
        numberOfSigners = 1
        outputs { 1 of MockAuditContract.Approval::class }
        timewindow()
    }

class MockAssetContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zknotary.common.zkp.MockAssetContract"
    }

    @BelongsToContract(MockAssetContract::class)
    data class MockAsset(
        override val owner: @Contextual AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {

        @FixedLength([1])
        override val participants: List<@Contextual AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(TestContract.Move(), copy(owner = newOwner))
    }

    class Issue : TypeOnlyCommandData(), ZKCommandData {
        @Transient
        val transactionMetadata = transactionMetadata {
            commands {
                +Issue::class
                +MockAuditContract.Approve::class
            }
        }

        @Transient
        override val metadata = commandMetadata {
            private = true
            numberOfSigners = 1
            outputs { 1 of MockAsset::class }
            timewindow()
        }

        @Transient
        override val circuit: CircuitMetaData = CircuitMetaData.fromConfig(File(""))
    }

    override fun verify(tx: LedgerTransaction) = TODO("Not yet implemented")
}

@ExperimentalTime
class ZKTransactionMetadataTest {
    // private val services = MockServices()

    @Test
    fun `On deposit requesting, the transaction must include the Request command`() {
        val approver = TestIdentity.fixed("Approver").party.anonymise()
        val issuer = TestIdentity.fixed("Issuer").party.anonymise()

        // services.zkLedger(zkService = MockZKTransactionService(services)) {
        //     zkTransaction {
        //         output(MockAuditContract.ID, MockAuditContract.Approval(approver))
        //         output(MockAssetContract.ID, MockAssetContract.MockAsset(issuer))
        //         command(approver.owningKey, MockAuditContract.Approve())
        //         command(issuer.owningKey, MockAssetContract.Issue())
        //         verifies()
        //     }
        // }
    }

    @Test
    fun `ZKTransactionMetadata DSL happy flow works`() {
        val transactionMetadata = transactionMetadata {
            network {
                attachmentConstraintType = HashAttachmentConstraint::class
            }

            commands {
                command(TestContract.Create::class)
                +TestContract.SignOnly::class // We also support this common patter for adding things to collections
            }
        }

        transactionMetadata.network.attachmentConstraintType shouldBe HashAttachmentConstraint::class
        transactionMetadata.commands[0] shouldBe TestContract.Create::class
        transactionMetadata.commands[1] shouldBe TestContract.SignOnly::class
    }

    @Test
    fun `Multiple commands of one type fails`() {
        shouldThrow<IllegalArgumentException> {
            transactionMetadata {
                commands {
                    +TestContract.SignOnly::class
                    +TestContract.SignOnly::class
                }
            }
        }
    }
}
