package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateRef
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.lazyMapped
import net.corda.core.serialization.serialize
import net.corda.core.transactions.ComponentGroup
import net.corda.core.utilities.OpaqueBytes

class ZKPartialMerkleTree(
    vtx: ZKVerifierTransaction,
    /**
     * For serialization of the leaves before hashing
     */
    val serializationFactoryService: SerializationFactoryService,
    val componentGroupLeafDigestService: DigestService,
    val nodeDigestService: DigestService
) : TransactionMerkleTree {
    val componentGroups: List<ComponentGroup>

    init {
        // construct the componentGroups based on our custom serialization
        componentGroups = buildComponentGroups(
            vtx.inputs,
            vtx.outputs,
            vtx.references,
            vtx.notary,
            vtx.timeWindow,
            vtx.networkParametersHash,
            vtx.serializationFactoryService
        )
    }

    override val root: SecureHash get() = tree.hash

    override val tree: MerkleTree by lazy { MerkleTree.getMerkleTree(groupHashes, nodeDigestService) }

    companion object {
        fun computeComponentHash(nonce: SecureHash, component: OpaqueBytes, digestService: DigestService): SecureHash =
            digestService.hash(nonce.bytes + component.bytes)

        private fun buildComponentGroups(
            inputs: List<ZKStateRef>,
            outputs: List<ZKStateRef>,
            references: List<ZKStateRef>,
            notary: Party?,
            timeWindow: TimeWindow?,
            networkParametersHash: SecureHash?,
            serializationFactoryService: SerializationFactoryService
        ): List<ComponentGroup> {
            val serialize = { value: Any, _: Int -> value.serialize(serializationFactoryService.factory) }

            val componentGroupMap: MutableList<ComponentGroup> = mutableListOf()
            if (inputs.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.INPUTS_GROUP.ordinal,
                    inputs.lazyMapped(serialize)
                )
            )
            if (references.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.REFERENCES_GROUP.ordinal,
                    references.lazyMapped(serialize)
                )
            )
            if (outputs.isNotEmpty()) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.OUTPUTS_GROUP.ordinal,
                    outputs.lazyMapped(serialize)
                )
            )
            if (notary != null) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.NOTARY_GROUP.ordinal,
                    listOf(notary).lazyMapped(serialize)
                )
            )
            if (timeWindow != null) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
                    listOf(timeWindow).lazyMapped(serialize)
                )
            )
            if (networkParametersHash != null) componentGroupMap.add(
                ComponentGroup(
                    ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
                    listOf(networkParametersHash).lazyMapped(serialize)
                )
            )
            return componentGroupMap
        }
    }

    /**
     * For each component group: the root hashes of the sub Merkle tree for that component group
     *
     * If a group's Merkle root is allOnesHash, it is a flag that denotes this group is empty (if list) or null (if single object)
     */
    internal val groupHashes: List<SecureHash> by lazy {
        val componentGroupHashes = mutableListOf<SecureHash>()
        // Even if empty and not used, we should at least send oneHashes for each known
        // or received but unknown (thus, bigger than known ordinal) component groups.
        for (i in 0..componentGroups.map { it.groupIndex }.max()!!) {
            val root = groupsMerkleRoots[i] ?: vtx.groupHashes[i]
            componentGroupHashes.add(root)
        }
        componentGroupHashes
    }

    /**
     * Calculate the root hashes of the component groups that are used to build the transaction's Merkle tree.
     * Each group has its own sub Merkle tree and the hash of the root of this sub tree works as a leaf of the top
     * level Merkle tree. The root of the latter is the transaction identifier.
     */
    private val groupsMerkleRoots: Map<Int, SecureHash> by lazy {
        componentHashes.map { (groupIndex: Int, componentHashesInGroup: List<SecureHash>) ->
            groupIndex to MerkleTree.getMerkleTree(
                componentHashesInGroup,
                nodeDigestService,
                componentGroupLeafDigestService
            ).hash
        }.toMap()
    }

    /**
     * Nonces for every transaction component in [componentGroups], including new fields (due to backwards compatibility support) we cannot process.
     * Nonce are computed in the following way:
     * nonce1 = H(salt || path_for_1st_component)
     * nonce2 = H(salt || path_for_2nd_component)
     * etc.
     * Thus, all of the nonces are "independent" in the sense that knowing one or some of them, you can learn nothing about the rest.
     */
    val componentNonces: Map<Int, List<SecureHash>> = vtx.componentNonces
    // val componentNonces: Map<Int, List<SecureHash>> by lazy {
    //     componentGroups.map { group ->
    //         group.groupIndex to group.components.mapIndexed { componentIndex, _ ->
    //             vtx.componentNonces[group.groupIndex]!![componentIndex]
    //         }
    //     }.toMap()
    // }

    /**
     * The hash for every transaction component, per component group. These will be used to build the full Merkle tree.
     */
    val componentHashes: Map<Int, List<SecureHash>> by lazy {
        componentGroups.map { group ->
            group.groupIndex to group.components.mapIndexed { componentIndex, component ->
                computeComponentHash(
                    componentNonces[group.groupIndex]!![componentIndex],
                    component,
                    componentGroupLeafDigestService
                )
            }
        }.toMap()
    }
}
