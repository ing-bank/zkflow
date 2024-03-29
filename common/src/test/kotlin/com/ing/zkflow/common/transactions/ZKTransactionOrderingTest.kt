package com.ing.zkflow.transactions

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.packageName
import com.ing.zkflow.common.transactions.UtxoInfo
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.crypto.BLAKE2S256
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.testing.withCustomSerializationEnv
import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.internal.withTestSerializationEnvIfNotSet
import net.corda.testing.node.MockServices
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertTrue

/**
 * Test the order of states imposed by the ZKTransactionBuilder. We would like to assert that the ordering of states
 * is consistent across ZKTransactionBuilder, in the WireTransaction that it generates, and in the Witness generated
 * from the WireTransaction.
 */
class ZKTransactionOrderingTest {
    private val outputs = mutableListOf<ContractState>()
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

        inputUtxoInfos.addAll(inputs.map { generateUtxoInfo(it) } as MutableList<UtxoInfo>)
        refUtxoInfos.addAll(refs.map { generateUtxoInfo(it.stateAndRef) })

        zkBuilder = ZKTransactionBuilder(notary, MockZKNetworkParameters()).apply {
            outputs.forEach { addOutputState(it, LocalContract.PROGRAM_ID, AlwaysAcceptAttachmentConstraint) }
            inputs.forEach { addInputState(it) }
            refs.forEach { addReferenceState(it) }
            addCommand(LocalContract.Create(), listOf(notary.owningKey))
        }

        wtx = createWireTransaction()
    }

    private fun generateUtxoInfo(stateAndRef: StateAndRef<ContractState>): UtxoInfo = withTestSerializationEnvIfNotSet {
        UtxoInfo.build(
            stateRef = stateAndRef.ref,
            serializedContents = stateAndRef.state.serialize().bytes,
            nonce = SecureHash.zeroHashFor(SecureHash.BLAKE2S256),
            stateClass = stateAndRef.state.data::class
        )
    }

    private fun createWireTransaction(): WireTransaction = withTestSerializationEnvIfNotSet {
        val cordappPackages = listOf(LocalContract.PROGRAM_ID.packageName).mapNotNull { it }
        val networkParameters = testNetworkParameters(minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION)
        val firstIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB"))
        val mockServices = MockServices(
            cordappPackages,
            firstIdentity,
            networkParameters
        )
        zkBuilder.toWireTransactionWithDefaultCordaSerializationForTesting(mockServices)
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
    fun `Witness ordering per command is consistent with WireTransaction`() = withCustomSerializationEnv {

        wtx.zkTransactionMetadata(ClassLoader.getSystemClassLoader()).commands.forEach { command ->
            val witness = Witness.fromWireTransaction(
                wtx = wtx,
                inputUtxoInfos = inputUtxoInfos,
                referenceUtxoInfos = refUtxoInfos,
                command
            )

            // compare outputs
            val actualDeserializedOutputs =
                witness.outputsGroup.map { it.serializedData }.map { it.deserialize<TransactionState<ContractState>>().data }

            val commandOutputIndices = command.outputs.map { it.index }
            val commandOutputs = wtx.outputStates.filterIndexed { index, _ ->
                commandOutputIndices.contains(index)
            }

            actualDeserializedOutputs.zip(commandOutputs) { a, b -> assertTrue(dummyStatesAreEqual(a, b)) }

            // compare utxos
            val commandInputIndices = command.inputs.map { it.index }
            val commandInputUtxos = inputUtxoInfos.filterIndexed { index, _ ->
                commandInputIndices.contains(index)
            }

            witness.serializedInputUtxos.map { it.serializedData }.forEachIndexed { index, bytes ->
                bytes shouldBe commandInputUtxos[index].serializedContents
            }

            val commandReferenceIndices = command.references.map { it.index }
            val commandReferenceUtxos = refUtxoInfos.filterIndexed { index, _ ->
                commandReferenceIndices.contains(index)
            }
            witness.serializedReferenceUtxos.map { it.serializedData }.forEachIndexed { index, bytes ->
                bytes shouldBe commandReferenceUtxos[index].serializedContents
            }
        }
    }
}

class LocalContract : Contract {
    companion object {
        const val PROGRAM_ID = "com.ing.zkflow.transactions.LocalContract"
    }

    @ZKP
    class Create : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            notary = true
            numberOfSigners = 1
            outputs {
                private(DummyZKStateA::class) at 0
                private(DummyZKStateA::class) at 1
                private(DummyZKStateB::class) at 2
                private(DummyZKStateB::class) at 3
            }
            inputs {
                any(DummyZKStateB::class) at 0
                any(DummyZKStateB::class) at 1
                any(DummyZKStateA::class) at 2
                any(DummyZKStateA::class) at 3
            }
            references {
                any(DummyZKStateB::class) at 0
                any(DummyZKStateB::class) at 1
                any(DummyState::class) at 2
                any(DummyZKStateA::class) at 3
                any(DummyZKStateA::class) at 4
            }
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    override fun verify(tx: LedgerTransaction) {}
}

object IntSetSerializer : FixedLengthSetSerializer<Int>(3, IntSerializer)
object ParticipantsSerializer : FixedLengthListSerializer<AnonymousParty>(1, AnonymousPartySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID))

@BelongsToContract(LocalContract::class)
@Suppress("EqualsWithHashCodeExist")
@Serializable
@ZKP
class DummyZKStateA(
    @Serializable(with = IntSerializer::class) val value: Int,
    @Serializable(with = IntSetSerializer::class) val set: Set<Int>,
    @Serializable(with = ParticipantsSerializer::class) override val participants: List<@Contextual AnonymousParty>
) : ContractState, VersionedContractStateGroup {
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

@BelongsToContract(LocalContract::class)
@Suppress("EqualsWithHashCodeExist")
@Serializable
@ZKP
class DummyZKStateB(
    @Serializable(with = IntSerializer::class) val value: Int,
    @Serializable(with = IntSetSerializer::class) val set: Set<Int>,
    @Serializable(with = ParticipantsSerializer::class) override val participants: List<@Contextual AnonymousParty>
) : ContractState, VersionedContractStateGroup {
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

@BelongsToContract(LocalContract::class)
@Serializable
@ZKP
data class DummyState(
    @Serializable(with = IntSerializer::class) val value: Int,
    @Serializable(with = IntSetSerializer::class) val set: Set<Int>,
    @Serializable(with = ParticipantsSerializer::class) override val participants: List<@Contextual AbstractParty>
) : ContractState, VersionedContractStateGroup {
    companion object {
        fun any(): DummyState {
            val alice = TestIdentity.fresh("Alice")
            return DummyState(
                Random.nextInt(),
                IntArray(Random.nextInt(1, 3)) { Random.nextInt() }.toSet(),
                listOf(alice.party)
            )
        }

        fun newTxState(): TransactionState<DummyState> {
            val notary = TestIdentity.fresh("Notary")

            return TransactionState(
                data = any(),
                notary = notary.party,
                encumbrance = 1,
                constraint = HashAttachmentConstraint(SecureHash.zeroHash)
            )
        }

        fun newTxState(notary: Party): TransactionState<DummyState> {
            return TransactionState(
                data = any(),
                notary = notary,
                encumbrance = 1,
                constraint = HashAttachmentConstraint(SecureHash.zeroHash)
            )
        }

        fun newStateAndRef(notary: Party): StateAndRef<DummyState> {
            return StateAndRef(newTxState(notary), StateRef(SecureHash.randomSHA256(), Random.nextInt(4)))
        }
    }
}
