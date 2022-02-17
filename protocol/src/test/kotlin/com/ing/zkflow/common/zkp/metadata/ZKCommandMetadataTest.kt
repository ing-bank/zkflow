package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.testing.fixtures.contract.TestContract.TestState
import com.ing.zkflow.testing.fixtures.state.DummyState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Random

class ZKCommandMetadataTest {

    @Test
    fun `ZKCommandMetadata DSL happy flow works`() {
        val cmd = object : ZKCommandData {
            override val metadata = commandMetadata {
                circuit { name = "foo" }

                numberOfSigners = 2

                inputs {
                    any(DummyState::class) at 0
                    any(TestState::class) at 1
                }
            }
        }

        cmd.metadata.shouldBeInstanceOf<ResolvedZKCommandMetadata>()
        cmd.metadata.circuit.name shouldBe "foo"
        cmd.metadata.privateInputs.size shouldBe 2
        cmd.metadata.privateInputs.first().type shouldBe DummyState::class
        cmd.metadata.privateInputs.first().index shouldBe 0
        cmd.metadata.privateInputs.last().type shouldBe TestState::class
        cmd.metadata.privateInputs.last().index shouldBe 1
    }

    @Test()
    fun `ZKCommandMetadata DSL rejects duplicate indexes`() {

        assertThrows<IllegalStateException> {
            object : ZKCommandData {
                override val metadata = commandMetadata {
                    circuit { name = "foo" }

                    numberOfSigners = 2

                    inputs {
                        any(DummyState::class) at 1
                        any(TestState::class) at 1
                    }
                }
            }
        }

        assertThrows<IllegalStateException> {
            object : ZKCommandData {
                override val metadata = commandMetadata {
                    circuit { name = "foo" }

                    numberOfSigners = 2

                    references {
                        any(DummyState::class) at 0
                        any(TestState::class) at 0
                    }
                }
            }
        }

        assertThrows<IllegalStateException> {
            object : ZKCommandData {
                override val metadata = commandMetadata {
                    circuit { name = "foo" }

                    numberOfSigners = 2

                    outputs {
                        private(DummyState::class) at 21
                        private(TestState::class) at 21
                    }
                }
            }
        }
    }
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
    class ThirdPartyNonZKPCommand : CommandData

    override fun verify(tx: LedgerTransaction) {}
}

class MockAuditContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zkflow.common.zkp.metadata.MockAuditContract"
    }

    @BelongsToContract(MockAuditContract::class)
    data class Approval(
        val approver: AnonymousParty
    ) : ZKContractState {
        override val participants: List<AnonymousParty> = listOf(approver)
    }

    /**
     * Audit records are not private, and therefore have no associated circuit.
     * If this command is used in a ZKFlow transaction, ZKFlow will still require
     * command metadata, so it can determine total component group/witness size.
     */
    class Approve : ZKCommandData {
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs { private(Approval::class) at 0 }
            timeWindow = true
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}

class MockAssetContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zkflow.common.zkp.metadata.MockAssetContract"
    }

    @BelongsToContract(MockAssetContract::class)
    data class MockAsset(
        override val owner: AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKOwnableState {
        override val participants: List<AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    class Move : ZKCommandData {
        override val metadata = commandMetadata {
            numberOfSigners = 2
            inputs { any(MockAsset::class) at 0 }
            outputs { private(MockAsset::class) at 0 }
            references { any(MockAuditContract.Approval::class) at 0 }
            timeWindow = true
        }
    }

    class IssueWithWrongCorDappCount : ZKCommandData {
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs { private(MockAsset::class) at 0 }
            timeWindow = true
        }
    }

    class Issue : ZKCommandData {
        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs { private(MockAsset::class) at 0 }
            timeWindow = true
        }
    }

    /**
     * This command demonstrates how to add ZKCommandData to third party commands with an extension function.
     * These extension functions will only be found if located within one of the know ZK commands in a transaction.
     */
    class IssueWithNonZKPCommand : ZKCommandData {
        companion object {
            @Suppress("unused") // found by reflection
            val MockThirdPartyNonZKPContract.ThirdPartyNonZKPCommand.metadata: ResolvedZKCommandMetadata
                get() = commandMetadata(this::class) {
                    numberOfSigners = 7
                    timeWindow = true
                }
        }

        override val metadata = commandMetadata {
            numberOfSigners = 1
            outputs { private(MockAsset::class) at 0 }
            timeWindow = true
        }
    }

    override fun verify(tx: LedgerTransaction) {
        tx.zkTransactionMetadata().verify(tx)
    }
}
