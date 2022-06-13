package com.ing.zkflow.common.transactions

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContract
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.contracts.filter
import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.ZKFlow.DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.packageName
import com.ing.zkflow.confirmExpectedVisibilityAfterFiltering
import com.ing.zkflow.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zkflow.testing.fixed
import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
import com.ing.zkflow.zkTransactionMetadata
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.Contract
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.BaseTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.internal.withTestSerializationEnvIfNotSet
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.util.Random

class FilteredLedgerTransactionTest {

    private val services = MockServices(
        listOfNotNull(LocalContract.PROGRAM_ID.packageName),
        TestIdentity.fixed("ServiceHub"),
        testNetworkParameters(minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION),
    )
    private val notary = TestIdentity.fixed("Notary").party
    private val alice = TestIdentity.fixed("Alice").party.anonymise()
    private val zkNetworkParameters = MockZKNetworkParameters(
        attachmentConstraintType = ZKAttachmentConstraintType.HashAttachmentConstraintType(DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM)
    )
    private val zkVerifierTransactionStorage = InMemoryZKVerifierTransactionStorage()

    class LocalContract : ZKContract, Contract {
        companion object {
            const val PROGRAM_ID = "com.ing.zkflow.common.transactions.LocalContract"
        }
    }

    @BelongsToContract(LocalContract::class)
    @ZKP
    data class TestState(
        val owner: AnonymousParty,
        val value: Int = Random().nextInt(1000)
    ) : ZKContractState, Versioned {
        override val participants: List<AnonymousParty> = listOf(owner)
    }

    @Test
    fun filteredLedgerTransactionsAreEqual() {
        withTestSerializationEnvIfNotSet {
            @ZKP
            class TestZKCommand : ZKCommandData, Versioned {
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

            val txbuilder = ZKTransactionBuilder(notary, zkNetworkParameters)
            txbuilder.addCommand(TestZKCommand(), alice.owningKey)
            txbuilder.addOutputState(publicOutput0) // at 0
            txbuilder.addOutputState(publicOutput1) // at 1
            txbuilder.addOutputState(privateOutput2) // at 2
            txbuilder.addOutputState(publicOutput3) // at 3
            txbuilder.addOutputState(privateOutput4) // at 4
            val wtx = txbuilder.toWireTransactionWithDefaultCordaSerializationForTesting(services)
            val proofs = mapOf<ZKCommandClassName, Proof>()
            val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

            vtx.verifyMerkleTree()

            vtx.outputHashes().size shouldBe 5
            vtx.outputs.size shouldBe 3

            fun confirmVisibility(tx: BaseTransaction) = tx.confirmExpectedVisibilityAfterFiltering(
                visible = listOf(publicOutput0, publicOutput1, publicOutput3),
                invisible = listOf(privateOutput2, privateOutput4)
            )

            confirmVisibility(vtx)

            val publicComponents = vtx.publicComponents(ComponentGroupEnum.OUTPUTS_GROUP)
            publicComponents.size shouldBe 3
            publicComponents.entries.forEachIndexed { index, componentWithIndex ->
                val serializedPublicComponent = componentWithIndex.value
                // Confirm that the public component entries as found in the vtx componentGroups appear in the vtx visible outputs in the same order
                serializedPublicComponent shouldBe vtx.componentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }.components[index]

                // Confirm that the public components as found in the vtx componentGroups are actually in that same index in the original wtx
                serializedPublicComponent shouldBe wtx.componentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }.components[componentWithIndex.key]
            }

            val filteredLtx = vtx.zkToFilteredLedgerTransaction(services, zkVerifierTransactionStorage)
            confirmVisibility(filteredLtx)

            val unfilteredLtx = wtx.toLedgerTransaction(services)
            val manuallyFilteredLtx = unfilteredLtx.filter(wtx.zkTransactionMetadata())
            confirmVisibility(manuallyFilteredLtx)

            manuallyFilteredLtx.outputs.size shouldBe filteredLtx.outputs.size
        }
    }
}
