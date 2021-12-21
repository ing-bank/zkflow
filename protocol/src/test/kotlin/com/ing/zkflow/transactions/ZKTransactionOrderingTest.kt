package com.ing.zkflow.transactions

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.transactions.UtxoInfo
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.packageName
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import com.ing.zkflow.crypto.BLAKE2S256
import com.ing.zkflow.serialization.CommandDataSerializerMap
import com.ing.zkflow.serialization.ContractStateSerializerMap
import com.ing.zkflow.testing.fixtures.contract.DummyContract
import com.ing.zkflow.testing.fixtures.state.DummyState
import com.ing.zkflow.testing.withCustomSerializationEnv
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertTrue

/**
 * Test the order of states imposed by the ZKTransactionBuilder. We would like to assert that the ordering of states by
 * classname is maintained in the ZKTransactionBuilder, in the WireTransaction that it generates, and in the Witness generated
 * from the WireTransaction.
 */
class ZKTransactionOrderingTest {
    private val outputs = mutableListOf<ZKContractState>()
    private val inputs = mutableListOf<StateAndRef<ContractState>>()
    private val inputUtxoInfos = mutableListOf<UtxoInfo>()
    private val refs = mutableListOf<ReferencedStateAndRef<ContractState>>()
    private val refUtxoInfos = mutableListOf<UtxoInfo>()
    private val zkBuilder: ZKTransactionBuilder
    private val wtx: WireTransaction

    init {

        val notary = TestIdentity.fresh("Notary").party

        // outputs
        val outputA1 = DummyZKStateA.newState()
        val outputA2 = DummyZKStateA.newState()
        val outputB1 = DummyZKStateB.newState()
        val outputB2 = DummyZKStateB.newState()
        outputs.addAll(listOf(outputA1, outputA2, outputB1, outputB2))

        // inputs
        val inputA1 = StateAndRef(DummyZKStateA.newTxState(notary), StateRef(SecureHash.randomSHA256(), 1))
        val inputA2 = StateAndRef(DummyZKStateA.newTxState(notary), StateRef(SecureHash.randomSHA256(), 1))
        val inputB1 = StateAndRef(DummyZKStateB.newTxState(notary), StateRef(SecureHash.randomSHA256(), 1))
        val inputB2 = StateAndRef(DummyZKStateB.newTxState(notary), StateRef(SecureHash.randomSHA256(), 2))
        inputs.addAll(listOf(inputB1, inputB2, inputA1, inputA2))

        // references
        val ref1 =
            ReferencedStateAndRef(StateAndRef(DummyState.newTxState(notary), StateRef(SecureHash.randomSHA256(), 3)))
        val refA1 =
            ReferencedStateAndRef(StateAndRef(DummyZKStateA.newTxState(notary), StateRef(SecureHash.randomSHA256(), 2)))
        val refA2 =
            ReferencedStateAndRef(StateAndRef(DummyZKStateA.newTxState(notary), StateRef(SecureHash.randomSHA256(), 2)))
        val refB1 =
            ReferencedStateAndRef(StateAndRef(DummyZKStateB.newTxState(notary), StateRef(SecureHash.randomSHA256(), 1)))
        val refB2 =
            ReferencedStateAndRef(StateAndRef(DummyZKStateB.newTxState(notary), StateRef(SecureHash.randomSHA256(), 1)))
        refs.addAll(listOf(refB1, refB2, ref1, refA1, refA2))

        withCustomSerializationEnv {
            inputUtxoInfos.addAll(inputs.map { generateUtxoInfo(it) } as MutableList<UtxoInfo>)
            refUtxoInfos.addAll(refs.map { generateUtxoInfo(it.stateAndRef) })
        }

        zkBuilder = ZKTransactionBuilder(notary).apply {
            outputs.forEach { addOutputState(it, LocalContract.PROGRAM_ID, AlwaysAcceptAttachmentConstraint) }
            inputs.forEach { addInputState(it) }
            refs.forEach { addReferenceState(it) }
            addCommand(LocalContract.Create(), listOf(notary.owningKey))
        }

        wtx = createWireTransaction()
    }

    private fun generateUtxoInfo(stateAndRef: StateAndRef<ContractState>): UtxoInfo {
        return UtxoInfo.build(
            stateRef = stateAndRef.ref,
            serializedContents = stateAndRef.state.serialize().bytes,
            nonce = SecureHash.zeroHashFor(SecureHash.BLAKE2S256),
            stateClass = stateAndRef.state.data::class
        )
    }

    private fun createWireTransaction(): WireTransaction = withCustomSerializationEnv {
        val cordappPackages = listOf(LocalContract.PROGRAM_ID.packageName, DummyContract.PROGRAM_ID.packageName).mapNotNull { it }
        val networkParameters = testNetworkParameters(minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION)
        val firstIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB"))
        val mockServices = MockServices(
            cordappPackages,
            firstIdentity,
            networkParameters
        )
        zkBuilder.toWireTransaction(mockServices)
    }

    /*
     Comparison function for dummy states. We use this instead of implementing equals() .
    */
    private fun dummyStatesAreEqual(first: Any?, second: Any?): Boolean {
        return if (first is DummyZKStateA && second is DummyZKStateA) {
            first.value == second.value && first.set == second.set && first.participants == second.participants
        } else if (first is DummyZKStateB && second is DummyZKStateB) {
            first.value == second.value && first.set == second.set && first.participants == second.participants
        } else if (first is DummyZKStateA && second is DummyZKStateB) {
            false
        } else if (first is DummyZKStateB && second is DummyZKStateA) {
            false
        } else {
            first == second
        }
    }

    @Test
    fun `Witness ordering is consistent with WireTransaction`() = withCustomSerializationEnv {
        val witness = Witness.fromWireTransaction(
            wtx = wtx,
            inputUtxoInfos = inputUtxoInfos,
            referenceUtxoInfos = refUtxoInfos
        )

        // compare inputs
        val actualDeserializedInputs = witness.inputsGroup.map { it.deserialize<StateRef>() }
        actualDeserializedInputs shouldBe wtx.inputs

        // compare outputs
        val actualDeserializedOutputs =
            witness.outputsGroup.flatMap { it.value }.map { it.deserialize<TransactionState<ContractState>>().data }
        actualDeserializedOutputs.zip(wtx.outputStates) { a, b -> assertTrue(dummyStatesAreEqual(a, b)) }

        // compare refs
        val actualDeserializedRefs = witness.referencesGroup.map { it.deserialize<StateRef>() }
        actualDeserializedRefs shouldBe wtx.references

        // compare utxos
        witness.serializedInputUtxos.flatMap { it.value }.forEachIndexed { index, bytes ->
            bytes shouldBe inputUtxoInfos[index].serializedContents
        }

        witness.serializedReferenceUtxos.flatMap { it.value }.forEachIndexed { index, bytes ->
            bytes shouldBe refUtxoInfos[index].serializedContents
        }
    }
}

class LocalContract : Contract {
    companion object {
        const val PROGRAM_ID = "com.ing.zkflow.transactions.LocalContract"
    }

    @Serializable
    class Create : ZKTransactionMetadataCommandData {
        init {
            CommandDataSerializerMap.register(this::class)
        }

        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
            commands { +Create::class }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            numberOfSigners = 1
            privateOutputs {
                0 private DummyZKStateA::class
                1 private DummyZKStateA::class
                2 private DummyZKStateB::class
                3 private DummyZKStateB::class
            }
            privateInputs {
                0 private DummyZKStateB::class
                1 private DummyZKStateB::class
                2 private DummyZKStateA::class
                3 private DummyZKStateA::class
            }
            privateReferences {
                0 mixed DummyZKStateB::class
                1 mixed DummyZKStateB::class
                2 mixed DummyState::class
                3 mixed DummyZKStateA::class
                4 mixed DummyZKStateA::class
            }
        }
    }

    override fun verify(tx: LedgerTransaction) {}
}

@Serializable
@BelongsToContract(LocalContract::class)
@Suppress("EqualsWithHashCodeExist")
class DummyZKStateA(
    val value: Int,
    @FixedLength([2]) val set: Set<Int>,
    @FixedLength([2]) override val participants: List<@Contextual AnonymousParty>
) : ZKContractState {
    init {
        ContractStateSerializerMap.register(this::class)
    }

    companion object {
        fun newState(): DummyZKStateA {
            val alice = TestIdentity.fresh("Alice")
            return DummyZKStateA(
                Random.nextInt(),
                IntArray(2) { Random.nextInt() }.toSet(),
                listOf(alice.party.anonymise())
            )
        }

        fun newTxState(notary: Party): TransactionState<DummyZKStateA> {
            return TransactionState(
                data = newState(),
                notary = notary,
                encumbrance = 1,
                constraint = AlwaysAcceptAttachmentConstraint
            )
        }
    }
}

@Serializable
@BelongsToContract(LocalContract::class)
@Suppress("EqualsWithHashCodeExist")
class DummyZKStateB(
    val value: Int,
    @FixedLength([2]) val set: Set<Int>,
    @FixedLength([2]) override val participants: List<@Contextual AnonymousParty>
) : ZKContractState {
    init {
        ContractStateSerializerMap.register(this::class)
    }

    companion object {
        fun newState(): DummyZKStateB {
            val alice = TestIdentity.fresh("Alice")
            return DummyZKStateB(
                Random.nextInt(),
                IntArray(2) { Random.nextInt() }.toSet(),
                listOf(alice.party.anonymise())
            )
        }

        fun newTxState(notary: Party): TransactionState<DummyZKStateB> {
            return TransactionState(
                data = newState(),
                notary = notary,
                encumbrance = 1,
                constraint = AlwaysAcceptAttachmentConstraint
            )
        }
    }
}
