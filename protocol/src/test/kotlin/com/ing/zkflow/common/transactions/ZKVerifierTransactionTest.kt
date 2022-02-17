package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.packageName
import com.ing.zkflow.testing.fixed
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.Contract
import net.corda.core.crypto.PartialMerkleTree
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.internal.withTestSerializationEnvIfNotSet
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.util.Random

class ZKVerifierTransactionTest {

    private val services = MockServices(
        listOfNotNull(LocalContract.PROGRAM_ID.packageName),
        TestIdentity.fixed("ServiceHub"),
        testNetworkParameters(minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION),
    )
    private val notary = TestIdentity.fixed("Notary").party
    private val alice = TestIdentity.fixed("Alice").party.anonymise()

    class LocalContract : Contract {
        companion object {
            const val PROGRAM_ID = "com.ing.zkflow.common.transactions.LocalContract"
        }

        override fun verify(tx: LedgerTransaction) {}
    }

    @BelongsToContract(LocalContract::class)
    data class TestState(
        val owner: AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKContractState {
        override val participants: List<AnonymousParty> = listOf(owner)
    }

    @Test
    fun testFilterOutputs1() {
        withTestSerializationEnvIfNotSet {
            class TestZKCommand : ZKCommandData {
                override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                    outputs {
                        private(TestState::class) at 1
                    }
                    numberOfSigners = 1
                }
            }

            val publicOutput = TestState(alice, 0)
            val privateOutput = TestState(alice, 1)

            val txbuilder = ZKTransactionBuilder(notary)
            txbuilder.addCommand(TestZKCommand(), alice.owningKey)
            txbuilder.addOutputState(publicOutput) // at 0
            txbuilder.addOutputState(privateOutput) // at 1
            val wtx = txbuilder.toWireTransactionWithDefaultCordaSerializationForTesting(services)
            val proofs = mapOf<ZKCommandClassName, Proof>()
            val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

            vtx.verify()

            vtx.outputHashes.size shouldBe 2
            vtx.outputs.size shouldBe 1

            val outputsGroup = vtx.filteredComponentGroups.find { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }!!
            outputsGroup.components.size shouldBe 1
            assert(outputsGroup.partialMerkleTree.root is PartialMerkleTree.PartialTree.Node)
            val rootNode = outputsGroup.partialMerkleTree.root as PartialMerkleTree.PartialTree.Node
            assert(rootNode.left is PartialMerkleTree.PartialTree.IncludedLeaf) // means that output 0 is public (included in a tx)
            assert(rootNode.right is PartialMerkleTree.PartialTree.Leaf) // means that output 1 is private

            vtx.outputs.map { it.data } shouldContain publicOutput
            vtx.outputs.map { it.data } shouldNotContain privateOutput
        }
    }

    @Test
    fun testFilterOutputs2() {
        withTestSerializationEnvIfNotSet {
            class TestZKCommand : ZKCommandData {
                override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                    outputs {
                        private(TestState::class) at 0
                    }
                    numberOfSigners = 1
                }
            }

            val privateOutput = TestState(alice, 0)
            val publicOutput1 = TestState(alice, 1)
            val publicOutput2 = TestState(alice, 2)
            val publicOutput3 = TestState(alice, 3)

            val txbuilder = ZKTransactionBuilder(notary)
            txbuilder.addCommand(TestZKCommand(), alice.owningKey)
            txbuilder.addOutputState(privateOutput) // at 0
            txbuilder.addOutputState(publicOutput1) // at 1
            txbuilder.addOutputState(publicOutput2) // at 2
            txbuilder.addOutputState(publicOutput3) // at 3
            val wtx = txbuilder.toWireTransactionWithDefaultCordaSerializationForTesting(services)
            val proofs = mapOf<ZKCommandClassName, Proof>()
            val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

            vtx.verify()

            vtx.outputHashes.size shouldBe 4
            vtx.outputs.size shouldBe 3

            vtx.outputs.map { it.data } shouldContain publicOutput1
            vtx.outputs.map { it.data } shouldContain publicOutput2
            vtx.outputs.map { it.data } shouldContain publicOutput3
            vtx.outputs.map { it.data } shouldNotContain privateOutput
        }
    }

    @Test
    fun testFilterOutputs3() {
        withTestSerializationEnvIfNotSet {
            class TestZKCommand : ZKCommandData {
                override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                    outputs {
                        private(TestState::class) at 2
                        public(TestState::class) at 3
                        private(TestState::class) at 4
                    }
                    numberOfSigners = 1
                }
            }

            val publicOutput0 = TestState(alice, 0)
            val publicOutput1 = TestState(alice, 1)
            val privateOutput2 = TestState(alice, 2)
            val publicOutput3 = TestState(alice, 3)
            val privateOutput4 = TestState(alice, 4)

            val txbuilder = ZKTransactionBuilder(notary)
            txbuilder.addCommand(TestZKCommand(), alice.owningKey)
            txbuilder.addOutputState(publicOutput0) // at 0
            txbuilder.addOutputState(publicOutput1) // at 1
            txbuilder.addOutputState(privateOutput2) // at 2
            txbuilder.addOutputState(publicOutput3) // at 3
            txbuilder.addOutputState(privateOutput4) // at 4
            val wtx = txbuilder.toWireTransactionWithDefaultCordaSerializationForTesting(services)
            val proofs = mapOf<ZKCommandClassName, Proof>()
            val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

            vtx.verify()

            vtx.outputHashes.size shouldBe 5
            vtx.outputs.size shouldBe 3

            vtx.outputs.map { it.data } shouldContain publicOutput0
            vtx.outputs.map { it.data } shouldContain publicOutput1
            vtx.outputs.map { it.data } shouldContain publicOutput3
            vtx.outputs.map { it.data } shouldNotContain privateOutput2
            vtx.outputs.map { it.data } shouldNotContain privateOutput4
        }
    }
}
