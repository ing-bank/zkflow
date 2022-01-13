package com.ing.zkflow.common.zkp.metadata

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.serialization.CommandDataSerializerMap
import com.ing.zkflow.serialization.ContractStateSerializerMap
import com.ing.zkflow.serialization.bfl.serializers.AnonymousPartySerializer
import com.ing.zkflow.testing.fixtures.contract.TestContract.TestState
import com.ing.zkflow.testing.fixtures.state.DummyState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import org.junit.jupiter.api.Test
import java.util.Random

class ZKCommandMetadataTest {

//    private val services = MockServices(
//        TestIdentity.fixed("ServiceHub"),
//        testNetworkParameters(minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION),
//    )
//    private val approver = TestIdentity.fixed("Approver").party.anonymise()
//    private val issuer = TestIdentity.fixed("Issuer").party.anonymise()
//    private val alice = TestIdentity.fixed("Alice").party.anonymise()

    @Test
    fun `ZKCommandMetadata DSL happy flow works`() {
        val cmd = object : ZKCommandData {
            override val metadata = commandMetadata {
                circuit { name = "foo" }

                numberOfSigners = 2

                inputs {
                    1 private DummyState::class
                    1 private TestState::class
                }
            }
        }

        cmd.metadata.shouldBeInstanceOf<ResolvedZKCommandMetadata>()
        cmd.metadata.circuit.name shouldBe "foo"
        cmd.metadata.privateInputs.size shouldBe 2
        cmd.metadata.privateInputs.first().type shouldBe DummyState::class
        cmd.metadata.privateInputs.first().index shouldBe 1
    }
}

val mockSerializers = run {
}

/**
 * MockNonZKPContract is a third party contract.
 * This means we can't annotate it, nor change its contents.
 */
class MockThirdPartyNonZKPContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zkflow.common.zkp.MockAuditContract"
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
        const val ID: ContractClassName = "com.ing.zkflow.common.zkp.metadata.MockAuditContract"
    }

    @Serializable
    @BelongsToContract(MockAuditContract::class)
    data class Approval(
        @Serializable(with = AnonymousPartySerializer::class)
        val approver: AnonymousParty
    ) : ZKContractState {
        init {
            ContractStateSerializerMap.register(this::class)
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
        init {
            CommandDataSerializerMap.register(this::class)
        }

        @Transient
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs { 1 private Approval::class }
            timeWindow = true
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}

class MockAssetContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zkflow.common.zkp.metadata.MockAssetContract"
    }

    @Serializable
    @BelongsToContract(MockAssetContract::class)
    data class MockAsset(
        @Serializable(with = AnonymousPartySerializer::class)
        override val owner: AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {

        init {
            ContractStateSerializerMap.register(this::class)
        }

        @FixedLength([1])
        override val participants: List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    @Serializable
    class Move : ZKCommandData {
        init {
            CommandDataSerializerMap.register(this::class)
        }

        @Transient
        override val metadata = commandMetadata {
            numberOfSigners = 2
            inputs { 1 private MockAsset::class }
            outputs { 1 private MockAsset::class }
            references { 1 private MockAuditContract.Approval::class }
            timeWindow = true

            network {
                attachmentConstraintType = SignatureAttachmentConstraint::class
            }
        }
    }

    @Serializable
    class IssueWithWrongCorDappCount : ZKCommandData {
        init {
            CommandDataSerializerMap.register(this::class)
        }

        @Transient
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs { 1 private MockAsset::class }
            timeWindow = true
        }
    }

    @Serializable
    class Issue : ZKCommandData {
        init {
            CommandDataSerializerMap.register(this::class)
        }

        @Transient
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs { 1 private MockAsset::class }
            timeWindow = true
        }
    }

    /**
     * This command demonstrates how to add ZKCommandData to third party commands with an extension function.
     * These extension functions will only be found if located within one of the know ZK commands in a transaction.
     */
    @Serializable
    class IssueWithNonZKPCommand : ZKCommandData {
        companion object {
            @Suppress("unused") // found by reflection
            val MockThirdPartyNonZKPContract.ThirdPartyNonZKPCommand.metadata: ResolvedZKCommandMetadata
                get() = commandMetadata(this::class) {
                    numberOfSigners = 7
                    timeWindow = true
                }
        }

        @Transient
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs { 1 private MockAsset::class }
            timeWindow = true
        }
    }

    override fun verify(tx: LedgerTransaction) {
        tx.zkTransactionMetadata().verify(tx)
    }
}
