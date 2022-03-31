package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.contracts.ZKOwnableState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
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
                    any(MockAuditContract.Approval::class) at 0
                    any(MockAssetContract.MockAsset::class) at 1
                }
            }
        }

        cmd.metadata.shouldBeInstanceOf<ResolvedZKCommandMetadata>()
        cmd.metadata.circuit.name shouldBe "foo"
        cmd.metadata.inputs.size shouldBe 2
        cmd.metadata.inputs.first().type shouldBe MockAuditContract.Approval::class
        cmd.metadata.inputs.first().index shouldBe 0
        cmd.metadata.inputs.last().type shouldBe MockAssetContract.MockAsset::class
        cmd.metadata.inputs.last().index shouldBe 1
    }

    @Test
    fun `ZKCommandMetadata DSL rejects duplicate indexes`() {

        assertThrows<IllegalStateException> {
            object : ZKCommandData {
                override val metadata = commandMetadata {
                    circuit { name = "foo" }

                    numberOfSigners = 2

                    inputs {
                        any(MockAuditContract.Approval::class) at 1
                        any(MockAssetContract.MockAsset::class) at 1
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
                        any(MockAuditContract.Approval::class) at 0
                        any(MockAssetContract.MockAsset::class) at 0
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
                        private(MockAuditContract.Approval::class) at 21
                        private(MockAssetContract.MockAsset::class) at 21
                    }
                }
            }
        }
    }
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
    @ZKP
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

    override fun verify(tx: LedgerTransaction) {
        // Contract verifications go here
    }
}
