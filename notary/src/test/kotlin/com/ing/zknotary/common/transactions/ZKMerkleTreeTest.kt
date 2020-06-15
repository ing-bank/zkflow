package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.ZKJsonSerializationFactoryService
import com.ing.zknotary.common.states.ZKReferenceStateRef
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.states.ZKStateRef
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.lazyMapped
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.ComponentGroup
import net.corda.core.transactions.NetworkParametersHash
import net.corda.core.transactions.ReferenceStateRef
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.nio.ByteBuffer
import java.util.function.Predicate
import kotlin.test.assertEquals

class ZKMerkleTreeTest {
    private val alice = TestIdentity.fresh("alice")
    private val bob = TestIdentity.fresh("bob")

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    @Test
    fun `can recalculate zkid based on serialized zkltx`() {
        ledgerServices.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            val ltx = wtx.toLedgerTransaction(ledgerServices)
            val serializationFactoryService = ZKJsonSerializationFactoryService()

            val ptx = ZKProverTransaction(
                ltx,
                serializationFactoryService,
                BLAKE2s256DigestService
            )

            val json = ptx.serialize(serializationFactoryService.factory)
            println(String(json.bytes))

            val deserializedPtx = json.deserialize(serializationFactoryService.factory)
            assertEquals(ptx, deserializedPtx)

            val json2 = ptx.serialize(serializationFactoryService.factory)
            assertEquals(json, json2)

            val witness = MockWitness(
                ptx.inputs,
                ptx.outputs,
                ptx.commands,
                ptx.attachments,
                ptx.notary,
                ptx.timeWindow,
                ptx.references,
                ptx.networkParametersHash
            )
            val proverInstance = MockInstance(
                ptx.id,
                inputsStateRefs = ltx.inputs.map { it.ref },
                referenceStateRefs = ltx.references.map { it.ref }
            )

            val proof = MockProof(witness, proverInstance)

            val vtx = ptx.toZKVerifierTransaction(
                Predicate {
                    it is ZKStateRef || it is ZKReferenceStateRef || it is TimeWindow || it == ptx.notary || it is NetworkParametersHash
                }
            )
            assertEquals(ptx.id, vtx.id)
            val amqp = vtx.serialize()

            val deserializedVtx = amqp.deserialize()
            assertEquals(vtx, deserializedVtx)

            // Next, we have to confirm that the visible parts of ftx and vtx match:
            val ftx = wtx.buildFilteredTransaction(
                Predicate {
                    it is StateRef || it is ReferenceStateRef || it is TimeWindow || it == ptx.notary || it is NetworkParametersHash
                }
            )

            /****************************************************
             * Verifier: receives FilteredTransaction (ftx) with extra payload: ZKVerifierTransaction (vtx) and proof
             ****************************************************/
            vtx.verify()
            ftx.verify()

            assertEquals(vtx.timeWindow, ftx.timeWindow)
            assertEquals(vtx.notary, ftx.notary)
            assertEquals(vtx.networkParametersHash, ftx.networkParametersHash)

            val verifierInstance = MockInstance(
                zkId = vtx.id,
                inputsStateRefs = ftx.inputs,
                referenceStateRefs = ftx.references
            )

            proof.verify(verifierInstance)
        }
    }

    data class MockWitness(
        var inputs: List<ZKStateAndRef<ContractState>>,
        val outputs: List<ZKStateAndRef<ContractState>>,
        val commands: List<Command<*>>,
        val attachments: List<SecureHash>,
        val notary: Party?,
        val timeWindow: TimeWindow?,
        val references: List<ZKStateAndRef<ContractState>>,
        val networkParametersHash: SecureHash?
    )

    data class MockInstance(
        val zkId: SecureHash,
        val inputsStateRefs: List<StateRef>,
        val referenceStateRefs: List<StateRef>
    )

    // This is the logic that should be in the proving circuit
    class MockProof(
        private val witness: MockWitness,
        instance: MockInstance
    ) {
        companion object {

            /**
             * IMPORTANT: these are the defined serialized components groups in a merkle tree.
             * The order must be respected, or the root will be different.
             *
             * Note: the commands are split into commands and signers. The reasons are obscure,
             * but we do it because Corda does it, right? ;-). See MerkleTransaction.kt:281
             */
            private enum class ComponentGroupEnum {
                INPUTS_GROUP, // ordinal = 0.
                OUTPUTS_GROUP, // ordinal = 1.
                COMMANDS_GROUP, // ordinal = 2.
                ATTACHMENTS_GROUP, // ordinal = 3.
                NOTARY_GROUP, // ordinal = 4.
                TIMEWINDOW_GROUP, // ordinal = 5.
                SIGNERS_GROUP, // ordinal = 6.
                REFERENCES_GROUP, // ordinal = 7.
                PARAMETERS_GROUP // ordinal = 8.
            }

            private fun computeComponentHash(nonce: ByteArray, component: ByteArray): ByteArray =
                BLAKE2s256DigestService.hash(nonce + component).bytes

            private fun computeNonce(privacySalt: ByteArray, groupIndex: Int, internalIndex: Int) =
                BLAKE2s256DigestService.hash(
                    privacySalt + ByteBuffer.allocate(8).putInt(groupIndex).putInt(internalIndex).array()
                )
        }

        init {
            verify(instance)
        }

        fun verify(instance: MockInstance) {
            // Do platform checks from TransactionVerifierServiceInternal.kt:44 that can't be done outside proof

            // serialize components of ZKProverTransaction
            val serializedComponents = buildSerializedComponentGroups(
                witness.inputs.map { it.ref },
                witness.outputs.map { it.ref },
                witness.commands,
                witness.attachments,
                witness.notary,
                witness.timeWindow,
                witness.references.map { it.ref },
                witness.networkParametersHash
            )

            // build Merkle tree with serialized components and compare root with instance.zkId

            // Confirm that the input/reference StateRefs from the instance are part of the ZKStateRefs at those locations
            // calculated_ZKStateRef_n = H(privacySalt + group + groupIdx + witness.ContractState_n + instance.StateRef_n)`.
            // Next: `calculated_ZKStateRef_n equals proof.merkletree.ZKStateRef_n`.

            // Check all required signatures are present

            // Verify contract business logic
        }

        private fun buildSerializedComponentGroups(
            inputs: List<ZKStateRef>,
            outputs: List<ZKStateRef>,
            commands: List<Command<*>>,
            attachments: List<SecureHash>,
            notary: Party?,
            timeWindow: TimeWindow?,
            references: List<ZKStateRef>,
            networkParametersHash: SecureHash?
        ): List<ComponentGroup> {
            // The serialization logic is hardcoded in the circuit
            val serialize = { value: Any, _: Int -> value.serialize(ZKJsonSerializationFactoryService().factory) }

            val componentGroupMap: MutableList<ComponentGroup> = mutableListOf()
            if (inputs.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.INPUTS_GROUP.ordinal,
                    inputs.lazyMapped(serialize)
                )
            )
            if (references.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.REFERENCES_GROUP.ordinal,
                    references.lazyMapped(serialize)
                )
            )
            if (outputs.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.OUTPUTS_GROUP.ordinal,
                    outputs.lazyMapped(serialize)
                )
            )
            // Adding commandData only to the commands group. Signers are added in their own group.
            if (commands.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.COMMANDS_GROUP.ordinal,
                    commands.map { it.value }.lazyMapped(serialize)
                )
            )
            if (attachments.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal,
                    attachments.lazyMapped(serialize)
                )
            )
            if (notary != null) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.NOTARY_GROUP.ordinal,
                    listOf(notary).lazyMapped(serialize)
                )
            )
            if (timeWindow != null) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
                    listOf(timeWindow).lazyMapped(serialize)
                )
            )
            // Adding signers to their own group. This is required for command visibility purposes: a party receiving
            // a FilteredTransaction can now verify it sees all the commands it should sign.
            if (commands.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.SIGNERS_GROUP.ordinal,
                    commands.map { it.signers }.lazyMapped(serialize)
                )
            )
            if (networkParametersHash != null) componentGroupMap.add(
                ComponentGroup(
                    net.corda.core.contracts.ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
                    listOf(networkParametersHash).lazyMapped(serialize)
                )
            )
            return componentGroupMap
        }
    }
}
