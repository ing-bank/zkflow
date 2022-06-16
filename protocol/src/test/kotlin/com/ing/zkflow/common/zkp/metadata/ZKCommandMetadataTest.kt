package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.versioning.Versioned
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Random

class ZKCommandMetadataTest {

    @Test
    fun `ZKCommandMetadata DSL happy flow works`() {
        val cmd = @ZKP object : ZKCommandData, Versioned {
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
            @ZKP object : ZKCommandData, Versioned {
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
            @ZKP object : ZKCommandData, Versioned {
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
            @ZKP object : ZKCommandData, Versioned {
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

    interface VersionedApproval : Versioned, ContractState

    @BelongsToContract(MockAuditContract::class)
    @ZKP
    data class Approval(
        val approver: AnonymousParty
    ) : VersionedApproval {
        override val participants: List<AnonymousParty> = listOf(approver)
    }

    override fun verify(tx: LedgerTransaction) {}
}

class MockAssetContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.ing.zkflow.common.zkp.metadata.MockAssetContract"
    }

    interface VersionedMockAsset : Versioned, OwnableState

    @BelongsToContract(MockAssetContract::class)
    @ZKP
    data class MockAsset(
        override val owner: AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : VersionedMockAsset {
        override val participants: List<AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
            require(newOwner is AnonymousParty)
            return CommandAndState(Move(), copy(owner = newOwner))
        }
    }

    interface VersionedMove : Versioned, ZKCommandData
    class Move : VersionedMove {
        override val metadata = commandMetadata {
            numberOfSigners = 2
            inputs { any(MockAsset::class) at 0 }
            outputs { private(MockAsset::class) at 0 }
            references { any(MockAuditContract.Approval::class) at 0 }
            timeWindow = true
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}
