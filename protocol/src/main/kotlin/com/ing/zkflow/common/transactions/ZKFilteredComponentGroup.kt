package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import net.corda.core.KeepForDJVM
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.ComponentGroup
import net.corda.core.transactions.FilteredComponentGroup
import net.corda.core.utilities.OpaqueBytes

@KeepForDJVM
@CordaSerializable
/**
 * ZKFilteredComponentGroup consists of public component serialized bytes (plus respective nonces) and hashes of private components.
 * When we build Merkle tree for the group we first place private hashes according to their indexes (key in map) and
 * then "fill holes" with public components hashes in the provided order.
 *
 * @privateComponentHashes maps private component index to component hash
 * @components contains only public components
 * @nonces contains only public components' nonces in the same order as in components list
 */
data class ZKFilteredComponentGroup(
    override val groupIndex: Int,
    override val components: List<OpaqueBytes>,
    private val nonces: List<SecureHash>,
    internal val privateComponentHashes: Map<Int, SecureHash>
) : ComponentGroup(groupIndex, components) {
    init {
        check(components.size == nonces.size) { "Size of transaction components and nonces do not match" }
        if (privateComponentHashes.isNotEmpty()) {
            check(privateComponentHashes.values.distinctBy { it.algorithm }.size == 1) { "All private components hashes should have the same algorithm" }
        }
    }

    private val groupSize = components.size + privateComponentHashes.size

    @Transient
    private var _merkleTree: MerkleTree? = null

    @Transient
    private var _allComponentHashes: List<SecureHash>? = null

    @Transient
    private var _publicComponents: Map<Int, OpaqueBytes>? = null

    fun merkleTree(digestService: DigestService): MerkleTree {
        if (_merkleTree == null) {
            synchronized(this) {
                if (_merkleTree == null) {
                    _merkleTree = calculateMerkleTree(digestService)
                }
            }
        }
        return _merkleTree!!
    }

    /**
     * Get the public components with their real indexes in the original WireTransaction.
     * This map's entries are in the order in which the components appear in the transaction, meaning that
     * the indexes of the map entries can be used to match them with the visible components in the ZKVerifierTransaction:
     */
    fun publicComponents(): Map<Int, OpaqueBytes> {
        if (_publicComponents == null) {
            synchronized(this) {
                if (_publicComponents == null) {
                    _publicComponents = calculatePublicComponents()
                }
            }
        }
        return _publicComponents!!
    }

    private fun publicComponentIndexes(): List<Int> {
        val usedPrivateIndexes = List(groupSize) { privateComponentHashes.containsKey(it) }
        return usedPrivateIndexes.mapIndexedNotNull { index, isPrivate -> if (!isPrivate) index else null }
    }

    private fun calculatePublicComponents(): Map<Int, OpaqueBytes> =
        publicComponentIndexes().foldIndexed(mapOf()) { publicComponentIndex, publicComponents, publicIndex ->
            publicComponents + (publicIndex to components[publicComponentIndex])
        }

    fun allComponentHashes(digestService: DigestService): List<SecureHash> {
        if (_allComponentHashes == null) {
            synchronized(this) {
                if (_allComponentHashes == null) {
                    _allComponentHashes = calculateComponentHashes(digestService)
                }
            }
        }
        return _allComponentHashes!!
    }

    private fun calculateMerkleTree(digestService: DigestService): MerkleTree {
        return MerkleTree.getMerkleTree(calculateComponentHashes(digestService), digestService)
    }

    private fun calculateComponentHashes(digestService: DigestService): List<SecureHash> {
        val allComponentHashes = arrayOfNulls<SecureHash?>(groupSize).toMutableList()

        // Put private components' hashes
        privateComponentHashes.entries.forEach { (index, hash) ->
            allComponentHashes[index] = hash
        }

        // Calculate public component hashes
        var publicComponentIndex = 0
        allComponentHashes.forEachIndexed { index: Int, hash: SecureHash? ->
            if (hash == null) {
                allComponentHashes[index] = digestService.componentHash(nonces[publicComponentIndex], components[publicComponentIndex])
                publicComponentIndex++
            }
        }

        return allComponentHashes.map { it!! }
    }

    companion object {
        fun fromComponentGroup(
            componentGroup: ComponentGroup,
            filteredComponentGroup: FilteredComponentGroup,
            zkTransactionMetadata: ResolvedZKTransactionMetadata?
        ): ZKFilteredComponentGroup {
            val groupIndex = componentGroup.groupIndex
            val allComponentNonces: List<SecureHash> = filteredComponentGroup.nonces

            return if (zkTransactionMetadata != null) {
                val visibleSerialisedComponents: MutableList<OpaqueBytes> = mutableListOf()
                val visibleComponentNonces: MutableList<SecureHash> = mutableListOf()
                val privateComponentHashes: MutableMap<Int, SecureHash> = LinkedHashMap()

                componentGroup.components.forEachIndexed { componentIndex, serialisedComponent ->
                    if (zkTransactionMetadata.shouldBeVisibleInFilteredComponentGroup(groupIndex, componentIndex)) {
                        visibleSerialisedComponents.add(serialisedComponent)
                        visibleComponentNonces.add(allComponentNonces[componentIndex])
                    } else {
                        privateComponentHashes[componentIndex] =
                            filteredComponentGroup.partialMerkleTree.getComponentHash(componentIndex)
                    }
                }

                ZKFilteredComponentGroup(
                    groupIndex = groupIndex,
                    components = visibleSerialisedComponents,
                    nonces = visibleComponentNonces,
                    privateComponentHashes = privateComponentHashes
                )
            } else {
                ZKFilteredComponentGroup(
                    groupIndex = groupIndex,
                    components = componentGroup.components,
                    nonces = allComponentNonces,
                    privateComponentHashes = emptyMap()
                )
            }
        }
    }
}
