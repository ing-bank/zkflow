package com.ing.zknotary.transactions

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.crypto.BLAKE2S256
import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.common.transactions.UtxoInfo
import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import com.ing.zknotary.common.zkp.CircuitMetaData
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.testing.fixtures.state.DummyState
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.minutes
import net.corda.core.utilities.seconds
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.makeTestIdentityService
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.test.assertTrue

object LocalSerializers {
    init {
        CommandDataSerializerMap.register(LocalContract.Create::class, 4000, LocalContract.Create.serializer())
    }
}

class ZKTransactionOrderingTest {
    /**
     * Test the order of states imposed by the ZKTransactionBuilder. We would like to assert that the ordering of states by
     * classname is maintained in the ZKTransactionBuilder, in the WireTransaction that it generates, and in the Witness generated
     * from the WireTransaction.
     */
    private val outputs = mutableListOf<ZKContractState>()
    private val expectedOrderedOutputs = mutableListOf<ZKContractState>()

    private val inputs = mutableListOf<StateAndRef<ContractState>>()
    private val expectedOrderedInputs = mutableListOf<StateAndRef<ContractState>>()
    private val inputUtxoInfos = mutableListOf<UtxoInfo>()
    private val expectedOrderedInputUtxoInfos = mutableListOf<UtxoInfo>()

    private val refs = mutableListOf<ReferencedStateAndRef<ContractState>>()
    private val expectedOrderedRefs = mutableListOf<ReferencedStateAndRef<ContractState>>()
    private val refUtxoInfos = mutableListOf<UtxoInfo>()
    private val expectedOrderedRefUtxoInfos = mutableListOf<UtxoInfo>()

    private val zkBuilder: ZKTransactionBuilder
    private val wtx: WireTransaction

    init {

        val notary = TestIdentity.fresh("Notary").party

        // outputs
        val outputA1 = DummyZKStateA.newState()
        val outputA2 = DummyZKStateA.newState()
        val outputB1 = DummyZKStateB.newState()
        val outputB2 = DummyZKStateB.newState()
        // shuffled
        outputs.addAll(listOf(outputB1, outputA1, outputA2, outputB2))
        // ordered
        expectedOrderedOutputs.addAll(listOf(outputA1, outputA2, outputB1, outputB2))

        // inputs
        val inputA1 = StateAndRef(DummyZKStateA.newTxState(notary), StateRef(SecureHash.randomSHA256(), 1))
        val inputA2 = StateAndRef(DummyZKStateA.newTxState(notary), StateRef(SecureHash.randomSHA256(), 1))
        val inputB1 = StateAndRef(DummyZKStateB.newTxState(notary), StateRef(SecureHash.randomSHA256(), 1))
        val inputB2 = StateAndRef(DummyZKStateB.newTxState(notary), StateRef(SecureHash.randomSHA256(), 2))
        // shuffled
        inputs.addAll(listOf(inputB1, inputA1, inputB2, inputA2))
        // ordered
        expectedOrderedInputs.addAll(listOf(inputA1, inputA2, inputB1, inputB2))

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
        // shuffled
        refs.addAll(listOf(refB1, refA1, refA2, refB2))
        // ordered
        expectedOrderedRefs.addAll(listOf(refA1, refA2, refB1, refB2))

        withCustomSerializationEnv {
            // utxo infos
            inputUtxoInfos.addAll(inputs.map { generateUtxoInfo(it) } as MutableList<UtxoInfo>)
            expectedOrderedInputUtxoInfos.addAll(expectedOrderedInputs.map { generateUtxoInfo(it) })
            refUtxoInfos.addAll(refs.map { generateUtxoInfo(it.stateAndRef) })
            expectedOrderedRefUtxoInfos.addAll(expectedOrderedRefs.map { generateUtxoInfo(it.stateAndRef) })
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
        return UtxoInfo(
            stateRef = stateAndRef.ref,
            serializedContents = stateAndRef.state.serialize().bytes,
            nonce = SecureHash.zeroHashFor(SecureHash.BLAKE2S256),
            stateName = stateAndRef.state.data::class.qualifiedName!!
        )
    }

    private fun createWireTransaction(): WireTransaction = withCustomSerializationEnv {
        val cordappPackages = listOf("com.ing.zknotary.transactions")
        val networkParameters = testNetworkParameters(minimumPlatformVersion = 10)
        val firstIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB"))
        val identityService = makeTestIdentityService()
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
    fun `Ordered ZKTransactionBuilder internally orders transaction states correctly`() {
        // test input state order
        zkBuilder.ordered().inputStates() shouldBe expectedOrderedInputs.map { it.ref }

        // test reference order
        zkBuilder.ordered().referenceStates() shouldBe expectedOrderedRefs.map { it.stateAndRef.ref }

        // test output state order
        zkBuilder.ordered().outputStates().map { it.data } shouldBe expectedOrderedOutputs
    }

    @Test
    fun `WireTransaction states are ordered correctly`() {
        wtx.outputStates shouldBe expectedOrderedOutputs
        wtx.inputs shouldBe expectedOrderedInputs.map { it.ref }
        wtx.references shouldBe expectedOrderedRefs.map { it.stateAndRef.ref }
    }

    @Test
    fun `Witness states are ordered correctly`() = withCustomSerializationEnv {
        val witness = Witness.fromWireTransaction(wtx, inputUtxoInfos, refUtxoInfos)

        // compare inputs
        val actualDeserializedInputs = witness.inputsGroup.map { it.deserialize<StateRef>() }
        actualDeserializedInputs shouldBe expectedOrderedInputs.map { it.ref }

        // compare outputs
        val actualDeserializedOutputs =
            witness.outputsGroup.flatMap { it.value }.map { it.deserialize<TransactionState<ContractState>>().data }
        actualDeserializedOutputs.zip(expectedOrderedOutputs) { a, b -> assertTrue(dummyStatesAreEqual(a, b)) }

        // compare refs
        val actualDeserializedRefs = witness.referencesGroup.map { it.deserialize<StateRef>() }
        actualDeserializedRefs shouldBe expectedOrderedRefs.map { it.stateAndRef.ref }

        // compare utxos
        witness.serializedInputUtxos.flatMap { it.value }.forEachIndexed { index, bytes ->
            bytes shouldBe expectedOrderedInputUtxoInfos[index].serializedContents
        }

        witness.serializedReferenceUtxos.flatMap { it.value }.forEachIndexed { index, bytes ->
            bytes shouldBe expectedOrderedRefUtxoInfos[index].serializedContents
        }
    }

    private fun <R> withCustomSerializationEnv(block: () -> R): R {
        return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv { block() }
    }
}

public class LocalContract : Contract {
    companion object {
        const val PROGRAM_ID = "com.ing.zknotary.transactions.LocalContract"
    }

    @Serializable
    public class Create : TypeOnlyCommandData(), ZKCommandData {

        init {
            LocalSerializers
        }

        @Transient
        override val circuit: CircuitMetaData = CircuitMetaData(
            name = "Foo",
            componentGroupSizes = mapOf(
                ComponentGroupEnum.OUTPUTS_GROUP to 4,
                ComponentGroupEnum.INPUTS_GROUP to 4,
                ComponentGroupEnum.REFERENCES_GROUP to 5,
                ComponentGroupEnum.COMMANDS_GROUP to 1,
                ComponentGroupEnum.NOTARY_GROUP to 1,
                ComponentGroupEnum.SIGNERS_GROUP to 1,
                ComponentGroupEnum.ATTACHMENTS_GROUP to 0,
                ComponentGroupEnum.PARAMETERS_GROUP to 0,
                ComponentGroupEnum.ATTACHMENTS_GROUP to 0

            ),
            javaClass2ZincType = mapOf(
                DummyState::class.qualifiedName!! to "ZincDummyState",
                DummyZKStateA::class.qualifiedName!! to "ZincDummyZKStateA",
                DummyZKStateB::class.qualifiedName!! to "ZincDummyZKStateB"

            ),
            buildFolder = File(""),
            buildTimeout = 10.seconds,
            setupTimeout = 2.minutes,
            provingTimeout = 2.minutes,
            verificationTimeout = 2.minutes,
        )
    }

    override fun verify(tx: LedgerTransaction) {
        if (tx.outputs.isEmpty()) throw IllegalArgumentException("Failed requirement: the tx has at least one output.")
        if (tx.inputs.isEmpty()) throw IllegalArgumentException("Failed requirement: the tx has at least an input.")
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}

@Serializable
@BelongsToContract(LocalContract::class)
@Suppress("EqualsWithHashCodeExist")
public class DummyZKStateA(
    val value: Int,
    @FixedLength([2]) val set: Set<Int>,
    @FixedLength([2]) override val participants: List<@Contextual AnonymousParty>
) : ZKContractState {

    init {
        DummyZKStateASerializer
    }

    public companion object {
        public fun newState(): DummyZKStateA {
            val alice = TestIdentity.fresh("Alice")
            return DummyZKStateA(
                Random.nextInt(),
                IntArray(2) { Random.nextInt() }.toSet(),
                listOf(alice.party.anonymise())
            )
        }

        public fun newTxState(notary: Party): TransactionState<DummyZKStateA> {
            return TransactionState(
                data = newState(),
                notary = notary,
                encumbrance = 1,
                constraint = AlwaysAcceptAttachmentConstraint
            )
        }
    }
}

public object DummyZKStateASerializer {
    init {
        ContractStateSerializerMap.register(DummyZKStateA::class, 2000, DummyZKStateA.serializer())
    }
}

@Serializable
@BelongsToContract(LocalContract::class)
@Suppress("EqualsWithHashCodeExist")
public class DummyZKStateB(
    val value: Int,
    @FixedLength([2]) val set: Set<Int>,
    @FixedLength([2]) override val participants: List<@Contextual AnonymousParty>
) : ZKContractState {

    init {
        DummyZKStateBSerializer
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    public companion object {
        public fun newState(): DummyZKStateB {
            val alice = TestIdentity.fresh("Alice")
            return DummyZKStateB(
                Random.nextInt(),
                IntArray(2) { Random.nextInt() }.toSet(),
                listOf(alice.party.anonymise())
            )
        }

        public fun newTxState(notary: Party): TransactionState<DummyZKStateB> {
            return TransactionState(
                data = DummyZKStateB.newState(),
                notary = notary,
                encumbrance = 1,
                constraint = AlwaysAcceptAttachmentConstraint
            )
        }
    }
}

public object DummyZKStateBSerializer {
    init {
        ContractStateSerializerMap.register(DummyZKStateB::class, 2001, DummyZKStateB.serializer())
    }
}
