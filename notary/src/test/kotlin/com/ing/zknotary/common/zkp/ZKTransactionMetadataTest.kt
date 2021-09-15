package com.ing.zknotary.common.zkp

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.contracts.ZKOwnableState
import com.ing.zknotary.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.common.serialization.bfl.serializers.AnonymousPartySerializer
import com.ing.zknotary.testing.dsl.zkLedger
import com.ing.zknotary.testing.fixed
import com.ing.zknotary.testing.fixtures.contract.TestContract
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.seconds
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Random
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ZKTransactionMetadataTest {
    private val services = MockServices()
    private val approver = TestIdentity.fixed("Approver").party.anonymise()
    private val issuer = TestIdentity.fixed("Issuer").party.anonymise()

    @Test
    fun `The transaction builder must match the resolved transaction metadata`() {
        services.zkLedger(zkService = MockZKTransactionService(services)) {
            zkTransaction {
                output(MockAssetContract.ID, MockAssetContract.MockAsset(issuer))
                fails()
                output(MockAuditContract.ID, MockAuditContract.Approval(approver))
                command(issuer.owningKey, MockAssetContract.Issue())
                fails()
                command(approver.owningKey, MockAuditContract.Approve())
                verifies()
            }
        }
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

val DUMMY_CIRCUIT_METADATA = CircuitMetaData(
    name = "Dummy",
    componentGroupSizes = mapOf(),
    javaClass2ZincType = emptyMap(),
    buildFolder = File(""),
    buildTimeout = 1.seconds,
    setupTimeout = 1.seconds,
    provingTimeout = 1.seconds,
    verificationTimeout = 1.seconds,
)

val mockSerializers = run {
    ContractStateSerializerMap.register(MockAuditContract.Approval::class, 9993, MockAuditContract.Approval.serializer())
    CommandDataSerializerMap.register(MockAssetContract.Issue::class, 9992, MockAssetContract.Issue.serializer())
    CommandDataSerializerMap.register(MockAuditContract.Approve::class, 9994, MockAuditContract.Approve.serializer())
    ContractStateSerializerMap.register(MockAssetContract.MockAsset::class, 9991, MockAssetContract.MockAsset.serializer())
}

/**
 * MockAuditContract is a third party contract.
 * This means we can't annotate it, nor change its contents.
 */
class MockAuditContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zknotary.common.zkp.MockAuditContract"
    }

    @Serializable
    @BelongsToContract(MockAuditContract::class)
    data class Approval(
        @Serializable(with = AnonymousPartySerializer::class)
        val approver: AnonymousParty
    ) : ZKContractState {
        init {
            // TODO: Hack!
            mockSerializers
        }

        @FixedLength([1])
        override val participants: List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(approver)
    }

    /**
     * Audit records are not private, and therefore have no associated circuit.
     * If this command is used in a ZKFlow transaction, ZKFlow will still require
     * command metadata, so it can determine total component group/witness size.
     */
    @Serializable
    class Approve : ZKCommandData {
        @Transient
        override val metadata = commandMetadata {
            private = true
            numberOfSigners = 1
            outputs { 1 of Approval::class }
            timewindow()
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}

class MockAssetContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zknotary.common.zkp.MockAssetContract"
    }

    @Serializable
    @BelongsToContract(MockAssetContract::class)
    data class MockAsset(
        @Serializable(with = AnonymousPartySerializer::class)
        override val owner: AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {

        @FixedLength([1])
        override val participants: List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(TestContract.Move(), copy(owner = newOwner))
    }

    @Serializable
    class Issue : ZKCommandData, ZKTransactionMetadataCommandData {
        @Transient
        override val transactionMetadata = transactionMetadata {
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
    }

    override fun verify(tx: LedgerTransaction) {}
}
