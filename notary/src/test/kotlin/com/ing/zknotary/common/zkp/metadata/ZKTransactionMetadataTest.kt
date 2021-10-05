package com.ing.zknotary.common.zkp.metadata

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.contracts.ZKOwnableState
import com.ing.zknotary.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.common.serialization.bfl.serializers.AnonymousPartySerializer
import com.ing.zknotary.common.transactions.zkFLowMetadata
import com.ing.zknotary.common.zkp.ZKFlow
import com.ing.zknotary.common.zkp.metadata.MockAssetContract.IssueWithNonZKPCommand.Companion.metadata
import com.ing.zknotary.common.zkp.metadata.ZKCommandList.Companion.ERROR_COMMAND_NOT_UNIQUE
import com.ing.zknotary.common.zkp.metadata.ZKTransactionMetadata.Companion.ERROR_COMMANDS_ALREADY_SET
import com.ing.zknotary.common.zkp.metadata.ZKTransactionMetadata.Companion.ERROR_NETWORK_ALREADY_SET
import com.ing.zknotary.testing.dsl.zkLedger
import com.ing.zknotary.testing.fixed
import com.ing.zknotary.testing.fixtures.contract.TestContract
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ZKTransactionMetadataTest {
    private val services = MockServices(
        TestIdentity.fixed("ServiceHub"),
        testNetworkParameters(minimumPlatformVersion = 10),
    )
    private val approver = TestIdentity.fixed("Approver").party.anonymise()
    private val issuer = TestIdentity.fixed("Issuer").party.anonymise()
    private val alice = TestIdentity.fixed("Alice").party.anonymise()

    @Test
    fun `The transaction builder must match structure from transaction metadata`() {
        services.zkLedger(zkService = MockZKTransactionService(services)) {
            // basic checks
            zkTransaction {
                output(MockAssetContract.ID, "Issued State", MockAssetContract.MockAsset(issuer))
                fails()
                output(MockAuditContract.ID, "Issue Approval", MockAuditContract.Approval(approver))
                command(issuer.owningKey, MockAssetContract.Issue())
                fails()
                command(approver.owningKey, MockAuditContract.Approve())
                verifies()
            }

            // also checks inputs and references
            zkTransaction {
                input("Issued State")
                reference("Issue Approval")
                output(MockAssetContract.ID, MockAssetContract.MockAsset(alice))
                fails()
                output(MockAuditContract.ID, MockAuditContract.Approval(approver))
                command(listOf(issuer.owningKey, alice.owningKey), MockAssetContract.Move())
                fails()
                command(approver.owningKey, MockAuditContract.Approve())
                verifies()
            }
        }
    }

    @Test
    fun `Delegated caching of resolved transaction metadata works transparently`() {
        class Command : ZKTransactionMetadataCommandData {
            override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
                commands {
                    +Command::class
                }
            }
            override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                private = true
                numberOfSigners = 3
            }
        }

        Command().transactionMetadata.commands.first().numberOfSigners shouldBe Command().metadata.numberOfSigners
        TransactionMetadataCache.resolvedTransactionMetadata[Command::class]?.commands?.first()?.commandKClass shouldBe Command::class

        class Command2 : ZKTransactionMetadataCommandData {
            override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
                commands {
                    +Command2::class
                }
            }
            override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                private = true
                numberOfSigners = 2
            }
        }

        Command2().transactionMetadata.commands.first().numberOfSigners shouldBe Command2().metadata.numberOfSigners
        TransactionMetadataCache.resolvedTransactionMetadata[Command2::class]?.commands?.first()?.commandKClass shouldBe Command2::class
    }

    private fun testUncachedTransactionMetadata(init: ZKTransactionMetadata.() -> Unit): ResolvedZKTransactionMetadata {
        return ZKTransactionMetadata().apply(init).resolve()
    }

    @Test
    fun `ZKTransactionMetadata DSL happy flow works`() {
        val transactionMetadata = testUncachedTransactionMetadata {
            network {
                attachmentConstraintType = SignatureAttachmentConstraint::class
            }

            commands {
                +TestContract.Create::class
                +TestContract.SignOnly::class
            }
        }

        transactionMetadata.network.attachmentConstraintType shouldBe SignatureAttachmentConstraint::class
        transactionMetadata.commands[0].commandKClass shouldBe TestContract.Create::class
        transactionMetadata.commands[1].commandKClass shouldBe TestContract.SignOnly::class
    }

    @Test
    fun `Diverging attachment constraints fails`() {
        shouldThrow<IllegalArgumentException> {
            testUncachedTransactionMetadata {
                network {
                    attachmentConstraintType = HashAttachmentConstraint::class
                }

                commands {
                    +TestContract.Create::class
                    +TestContract.SignOnly::class
                }
            }
        }.also {
            it.message shouldStartWith ResolvedZKTransactionMetadata.ERROR_ATTACHMENT_CONSTRAINT_DOES_NOT_MATCH
        }
    }

    @Test
    fun `Wrong cordapp count fails`() {
        services.zkLedger(zkService = MockZKTransactionService(services)) {
            zkTransaction {
                output(MockAssetContract.ID, MockAssetContract.MockAsset(issuer))
                command(listOf(issuer.owningKey), MockAssetContract.IssueWithWrongCorDappCount())
                failsWith("Expected ${MockAssetContract.IssueWithWrongCorDappCount().transactionMetadata.numberOfCorDappsForContracts} contract attachments, found 1")
            }
        }
    }

    @Test
    fun `No commands fails`() {
        shouldThrow<IllegalArgumentException> {
            testUncachedTransactionMetadata {}
        }.also {
            it.message shouldBe ResolvedZKTransactionMetadata.ERROR_NO_COMMANDS
        }
    }

    @Test
    fun `Metadata extension function is found`() {
        val metadata = testUncachedTransactionMetadata {
            commands {
                +MockAssetContract.IssueWithNonZKPCommand::class
                +MockThirdPartyNonZKPContract.ThirdPartyNonZKPCommand::class
            }
        }

        val expectedMetadata = MockThirdPartyNonZKPContract.ThirdPartyNonZKPCommand().metadata
        metadata.commands[1].commandKClass shouldBe MockThirdPartyNonZKPContract.ThirdPartyNonZKPCommand::class
        metadata.commands[1].numberOfSigners shouldBe expectedMetadata.numberOfSigners
    }

    @Test
    fun `Adding network multiple times is illegal`() {
        shouldThrow<IllegalStateException> {
            testUncachedTransactionMetadata {
                network {
                    attachmentConstraintType = HashAttachmentConstraint::class
                }
                network {
                    participantSignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
                }
            }
        }.also {
            it.message shouldBe ERROR_NETWORK_ALREADY_SET
        }
    }

    @Test
    fun `Adding commands multiple times is illegal`() {
        shouldThrow<IllegalStateException> {
            testUncachedTransactionMetadata {
                commands {
                    +MockAssetContract.Issue::class
                }
                commands {
                    +MockThirdPartyNonZKPContract.ThirdPartyNonZKPCommand::class
                }
            }
        }.also {
            it.message shouldBe ERROR_COMMANDS_ALREADY_SET
        }
    }

    @Test
    fun `Multiple commands of one type fails`() {
        shouldThrow<IllegalArgumentException> {
            testUncachedTransactionMetadata {
                commands {
                    +MockAssetContract.Issue::class
                    +MockAssetContract.Issue::class
                }
            }
        }.also {
            it.message shouldBe ERROR_COMMAND_NOT_UNIQUE
        }
    }
}

val mockSerializers = run {
    ContractStateSerializerMap.register(MockAuditContract.Approval::class, 9993, MockAuditContract.Approval.serializer())
    CommandDataSerializerMap.register(MockAssetContract.Issue::class, 9992, MockAssetContract.Issue.serializer())
    CommandDataSerializerMap.register(
        MockAssetContract.IssueWithWrongCorDappCount::class,
        99998,
        MockAssetContract.IssueWithWrongCorDappCount.serializer()
    )
    CommandDataSerializerMap.register(MockAssetContract.Move::class, 9996, MockAssetContract.Move.serializer())
    CommandDataSerializerMap.register(MockAuditContract.Approve::class, 9994, MockAuditContract.Approve.serializer())
    ContractStateSerializerMap.register(MockAssetContract.MockAsset::class, 9991, MockAssetContract.MockAsset.serializer())
}

/**
 * MockNonZKPContract is a third party contract.
 * This means we can't annotate it, nor change its contents.
 */
class MockThirdPartyNonZKPContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zknotary.common.zkp.MockAuditContract"
    }

    /**
     * This command is third party, and not ZKCommandData.
     * If this command is used in a ZKFlow transaction, ZKFlow will still require
     * command metadata, so it can determine total component group/witness size.
     * It will look for extension functions defined in known ZKCommandData classes.
     */
    @Serializable
    class ThirdPartyNonZKPCommand : CommandData

    override fun verify(tx: LedgerTransaction) {}
}

class MockAuditContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zknotary.common.zkp.metadata.MockAuditContract"
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
            numberOfSigners = 1
            outputs { 1 of Approval::class }
            timewindow()
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}

class MockAssetContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zknotary.common.zkp.metadata.MockAssetContract"
    }

    @Serializable
    @BelongsToContract(MockAssetContract::class)
    data class MockAsset(
        @Serializable(with = AnonymousPartySerializer::class)
        override val owner: AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {

        init {
            // TODO: Hack!
            mockSerializers
        }

        @FixedLength([1])
        override val participants: List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(TestContract.Move(), copy(owner = newOwner))
    }

    @Serializable
    class Move : ZKCommandData, ZKTransactionMetadataCommandData {
        override val transactionMetadata by transactionMetadata {
            network { attachmentConstraintType = SignatureAttachmentConstraint::class }
            commands {
                +Move::class
                +MockAuditContract.Approve::class
            }
        }

        @Transient
        override val metadata = commandMetadata {
            private = true
            numberOfSigners = 2
            inputs { 1 of MockAsset::class }
            outputs { 1 of MockAsset::class }
            references { 1 of MockAuditContract.Approval::class }
            timewindow()
        }
    }

    @Serializable
    class IssueWithWrongCorDappCount : ZKTransactionMetadataCommandData {
        override val transactionMetadata by transactionMetadata {
            commands {
                +IssueWithWrongCorDappCount::class
            }
            numberOfCorDappsForContracts = 2
        }

        @Transient
        override val metadata = commandMetadata {
            private = true
            numberOfSigners = 1
            outputs { 1 of MockAsset::class }
            timewindow()
        }
    }

    @Serializable
    class Issue : ZKTransactionMetadataCommandData {
        override val transactionMetadata by transactionMetadata {
            network { attachmentConstraintType = SignatureAttachmentConstraint::class }
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

    /**
     * This command demonstrates how to add ZKCommandData to third party commands with an extension function.
     * These extension functions will only be found if located within one of the know ZK commands in a transaction.
     */
    @Serializable
    class IssueWithNonZKPCommand : ZKCommandData, ZKTransactionMetadataCommandData {
        companion object {
            @Suppress("unused") // found by reflection
            val MockThirdPartyNonZKPContract.ThirdPartyNonZKPCommand.metadata: ResolvedZKCommandMetadata
                get() = commandMetadata(this::class) {
                    numberOfSigners = 7
                    timewindow()
                }
        }

        override val transactionMetadata by transactionMetadata {
            commands {
                +IssueWithNonZKPCommand::class
                +MockThirdPartyNonZKPContract.ThirdPartyNonZKPCommand::class
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

    override fun verify(tx: LedgerTransaction) {
        tx.zkFLowMetadata.verify(tx)
    }
}
