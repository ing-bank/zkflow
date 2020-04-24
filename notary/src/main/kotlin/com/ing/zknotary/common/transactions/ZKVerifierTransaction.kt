package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKReferenceStateRef
import com.ing.zknotary.common.states.ZKStateRef
import java.security.PublicKey
import java.util.function.Predicate
import net.corda.core.KeepForDJVM
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.PartialMerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.verify
import net.corda.core.identity.Party
import net.corda.core.internal.deserialiseCommands
import net.corda.core.internal.deserialiseComponentGroup
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.ComponentGroup
import net.corda.core.transactions.ComponentVisibilityException
import net.corda.core.transactions.FilteredComponentGroup
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.FilteredTransactionVerificationException
import net.corda.core.transactions.NetworkParametersHash
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes

@KeepForDJVM
@CordaSerializable
class ZKVerifierTransaction(
    val id: SecureHash,
    private val filteredComponentGroups: List<FilteredComponentGroup>,
    private val groupHashes: List<SecureHash>,
    private val serializationFactoryService: SerializationFactoryService,
    private val nodeDigestService: DigestService,
    private val componentGroupLeafDigestService: DigestService
) {
    // This whole block of vars is directly lifted from TraversableTransaction.
    val inputs: List<ZKStateRef> = deserialiseComponentGroup(
        filteredComponentGroups,
        ZKStateRef::class,
        ComponentGroupEnum.INPUTS_GROUP,
        factory = serializationFactoryService.factory
    )
    val references: List<ZKStateRef> =
        deserialiseComponentGroup(
            filteredComponentGroups, ZKStateRef::class, ComponentGroupEnum.REFERENCES_GROUP,
            factory = serializationFactoryService.factory
        )
    val outputs: List<ZKStateRef> =
        deserialiseComponentGroup(
            filteredComponentGroups, ZKStateRef::class, ComponentGroupEnum.OUTPUTS_GROUP,
            factory = serializationFactoryService.factory
        )
    val commands: List<Command<*>> = deserialiseCommands(
        filteredComponentGroups,
        factory = serializationFactoryService.factory
    )
    val notary: Party? = let {
        val notaries: List<Party> =
            deserialiseComponentGroup(
                filteredComponentGroups, Party::class, ComponentGroupEnum.NOTARY_GROUP,
                factory = serializationFactoryService.factory
            )
        check(notaries.size <= 1) { "Invalid Transaction. More than 1 notary party detected." }
        notaries.firstOrNull()
    }
    val timeWindow: TimeWindow? = let {
        val timeWindows: List<TimeWindow> =
            deserialiseComponentGroup(
                filteredComponentGroups, TimeWindow::class, ComponentGroupEnum.TIMEWINDOW_GROUP,
                factory = serializationFactoryService.factory
            )
        check(timeWindows.size <= 1) { "Invalid Transaction. More than 1 time-window detected." }
        timeWindows.firstOrNull()
    }
    val networkParametersHash: SecureHash? = let {
        val parametersHashes =
            deserialiseComponentGroup(
                filteredComponentGroups, SecureHash::class, ComponentGroupEnum.PARAMETERS_GROUP,
                factory = serializationFactoryService.factory
            )
        check(parametersHashes.size <= 1) { "Invalid Transaction. More than 1 network parameters hash detected." }
        parametersHashes.firstOrNull()
    }
    val attachments: List<SecureHash> =
        deserialiseComponentGroup(
            filteredComponentGroups, SecureHash::class, ComponentGroupEnum.ATTACHMENTS_GROUP,
            factory = serializationFactoryService.factory
        )

    private val availableComponentGroups: List<List<Any>>
        get() {
            val result = mutableListOf(inputs, outputs, commands, attachments, references)
            notary?.let { result += listOf(it) }
            timeWindow?.let { result += listOf(it) }
            networkParametersHash?.let { result += listOf(it) }
            return result
        }

    companion object {

        @JvmStatic
        fun buildFilteredTransaction(
            zkProverTransaction: ZKProverTransaction,
            filtering: Predicate<Any>
        ): ZKVerifierTransaction {
            val filteredComponentGroups = filterWithFun(zkProverTransaction, filtering)
            return ZKVerifierTransaction(
                zkProverTransaction.id,
                filteredComponentGroups,
                zkProverTransaction.merkleTree.groupHashes,
                zkProverTransaction.serializationFactoryService,
                zkProverTransaction.merkleTree.nodeDigestService,
                zkProverTransaction.merkleTree.componentGroupLeafDigestService
            )
        }

        /**
         * Construction of partial transaction from [ZKProverTransaction] based on filtering.
         * Note that list of nonces to be sent is updated on the fly, based on the index of the filtered tx component.
         * @param filtering filtering over the whole ZKProverTransaction.
         * @return a list of [FilteredComponentGroup] used in PartialMerkleTree calculation and verification.
         */
        private fun filterWithFun(ptx: ZKProverTransaction, filtering: Predicate<Any>): List<FilteredComponentGroup> {
            val filteredSerialisedComponents: MutableMap<Int, MutableList<OpaqueBytes>> = hashMapOf()
            val filteredComponentNonces: MutableMap<Int, MutableList<SecureHash>> = hashMapOf()
            val filteredComponentHashes: MutableMap<Int, MutableList<SecureHash>> =
                hashMapOf() // Required for partial Merkle tree generation.
            var signersIncluded = false

            fun <T : Any> filter(t: T, componentGroupIndex: Int, internalIndex: Int) {
                if (!filtering.test(t)) return

                val group = filteredSerialisedComponents[componentGroupIndex]
                // Because the filter passed, we know there is a match. We also use first Vs single as the init function
                // of ZKProverTransaction ensures there are no duplicated groups.
                val serialisedComponent =
                    ptx.merkleTree.componentGroups.first { it.groupIndex == componentGroupIndex }.components[internalIndex]
                if (group == null) {
                    // As all of the helper Map structures, like availableComponentNonces, availableComponentHashes
                    // and groupsMerkleRoots, are computed lazily via componentGroups.forEach, there should always be
                    // a match on Map.get ensuring it will never return null.
                    filteredSerialisedComponents[componentGroupIndex] = mutableListOf(serialisedComponent)
                    filteredComponentNonces[componentGroupIndex] =
                        mutableListOf(ptx.merkleTree.componentNonces[componentGroupIndex]!![internalIndex])
                    filteredComponentHashes[componentGroupIndex] =
                        mutableListOf(ptx.merkleTree.componentHashes[componentGroupIndex]!![internalIndex])
                } else {
                    group.add(serialisedComponent)
                    // If the group[componentGroupIndex] existed, then we guarantee that
                    // filteredComponentNonces[componentGroupIndex] and filteredComponentHashes[componentGroupIndex] are not null.
                    filteredComponentNonces[componentGroupIndex]!!.add(ptx.merkleTree.componentNonces[componentGroupIndex]!![internalIndex])
                    filteredComponentHashes[componentGroupIndex]!!.add(ptx.merkleTree.componentHashes[componentGroupIndex]!![internalIndex])
                }
                // If at least one command is visible, then all command-signers should be visible as well.
                // This is required for visibility purposes, see FilteredTransaction.checkAllCommandsVisible() for more details.
                if (componentGroupIndex == ComponentGroupEnum.COMMANDS_GROUP.ordinal && !signersIncluded) {
                    signersIncluded = true
                    val signersGroupIndex = ComponentGroupEnum.SIGNERS_GROUP.ordinal
                    // There exist commands, thus the signers group is not empty.
                    val signersGroupComponents =
                        ptx.merkleTree.componentGroups.first { it.groupIndex == signersGroupIndex }
                    filteredSerialisedComponents[signersGroupIndex] = signersGroupComponents.components.toMutableList()
                    filteredComponentNonces[signersGroupIndex] =
                        ptx.merkleTree.componentNonces[signersGroupIndex]!!.toMutableList()
                    filteredComponentHashes[signersGroupIndex] =
                        ptx.merkleTree.componentHashes[signersGroupIndex]!!.toMutableList()
                }
            }

            fun updateFilteredComponents() {
                ptx.inputs.forEachIndexed { internalIndex, it ->
                    filter(
                        it.ref, // Important: even though the ZKProverTransaction contains the full inputs, only their ZKStateRefs are used for the merkle tree
                        ComponentGroupEnum.INPUTS_GROUP.ordinal,
                        internalIndex
                    )
                }
                ptx.outputs.forEachIndexed { internalIndex, it ->
                    filter(
                        it.ref, // Important: even though the ZKProverTransaction contains the full inputs, only their ZKStateRefs are used for the merkle tree
                        ComponentGroupEnum.OUTPUTS_GROUP.ordinal,
                        internalIndex
                    )
                }
                ptx.commands.forEachIndexed { internalIndex, it ->
                    filter(
                        it,
                        ComponentGroupEnum.COMMANDS_GROUP.ordinal,
                        internalIndex
                    )
                }
                ptx.attachments.forEachIndexed { internalIndex, it ->
                    filter(
                        it,
                        ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal,
                        internalIndex
                    )
                }
                if (ptx.notary != null) filter(ptx.notary, ComponentGroupEnum.NOTARY_GROUP.ordinal, 0)
                if (ptx.timeWindow != null) filter(ptx.timeWindow, ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal, 0)
                // Note that because [inputs] and [references] share the same type [StateRef], we use a wrapper for references [ReferenceStateRef],
                // when filtering. Thus, to filter-in all [references] based on type, one should use the wrapper type [ReferenceStateRef] and not [StateRef].
                // Similar situation is for network parameters hash and attachments, one should use wrapper [NetworkParametersHash] and not [SecureHash].
                ptx.references.forEachIndexed { internalIndex, it ->
                    filter(
                        ZKReferenceStateRef(it.ref),
                        ComponentGroupEnum.REFERENCES_GROUP.ordinal,
                        internalIndex
                    )
                }
                ptx.networkParametersHash?.let {
                    filter(
                        NetworkParametersHash(it),
                        ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
                        0
                    )
                }
                // It is highlighted that because there is no a signers property in TraversableTransaction,
                // one cannot specifically filter them in or out.
                // The above is very important to ensure someone won't filter out the signers component group if at least one
                // command is included in a FilteredTransaction.

                // It's sometimes possible that when we receive a ZKProverTransaction for which there is a new or more unknown component groups,
                // we decide to filter and attach this field to a FilteredTransaction.
                // An example would be to redact certain contract state types, but otherwise leave a transaction alone,
                // including the unknown new components.
                ptx.merkleTree.componentGroups
                    .filter { it.groupIndex >= ComponentGroupEnum.values().size }
                    .forEach { componentGroup ->
                        componentGroup.components.forEachIndexed { internalIndex, component ->
                            filter(
                                component,
                                componentGroup.groupIndex,
                                internalIndex
                            )
                        }
                    }
            }

            fun createPartialMerkleTree(componentGroupIndex: Int): PartialMerkleTree {
                return PartialMerkleTree.build(
                    MerkleTree.getMerkleTree(
                        ptx.merkleTree.componentHashes[componentGroupIndex]!!,
                        ptx.merkleTree.nodeDigestService,
                        ptx.merkleTree.componentGroupLeafDigestService
                    ),
                    filteredComponentHashes[componentGroupIndex]!!
                )
            }

            fun createFilteredComponentGroups(): List<FilteredComponentGroup> {
                updateFilteredComponents()
                val filteredComponentGroups: MutableList<FilteredComponentGroup> = mutableListOf()
                filteredSerialisedComponents.forEach { (groupIndex, value) ->
                    filteredComponentGroups.add(
                        FilteredComponentGroup(
                            groupIndex,
                            value,
                            filteredComponentNonces[groupIndex]!!,
                            createPartialMerkleTree(groupIndex)
                        )
                    )
                }
                return filteredComponentGroups
            }

            return createFilteredComponentGroups()
        }
    }

    /**
     * Runs verification of partial Merkle branch against [id].
     * Note that empty filtered transactions (with no component groups) are accepted as well,
     * e.g. for Timestamp Authorities to blindly sign or any other similar case in the future
     * that requires a blind signature over a transaction's [id].
     * @throws ZKVerifierTransactionVerificationException if verification fails.
     */
    @Throws(ZKVerifierTransactionVerificationException::class)
    fun verify() {
        verificationCheck(groupHashes.isNotEmpty()) { "At least one component group hash is required" }
        // Verify the top level Merkle tree (group hashes are its leaves, including allOnesHashå for empty list or null
        // components in WireTransaction).

        // TODO: use the correct digest service.
        verificationCheck(
            MerkleTree.getMerkleTree(
                groupHashes,
                nodeDigestService,
                componentGroupLeafDigestService
            ).hash == id
        ) {
            "Top level Merkle tree cannot be verified against transaction's id"
        }

        // For completely blind verification (no components are included).
        if (filteredComponentGroups.isEmpty()) return

        // Compute partial Merkle roots for each filtered component and verify each of the partial Merkle trees.
        filteredComponentGroups.forEach { (groupIndex, components, nonces, groupPartialTree) ->
            verificationCheck(groupIndex < groupHashes.size) { "There is no matching component group hash for group $groupIndex" }
            val groupMerkleRoot = groupHashes[groupIndex]
            verificationCheck(
                groupMerkleRoot == PartialMerkleTree.rootAndUsedHashes(
                    groupPartialTree.root,
                    mutableListOf()
                )
            ) {
                "Partial Merkle tree root and advertised full Merkle tree root for component group $groupIndex do not match"
            }
            verificationCheck(
                groupPartialTree.verify(
                    groupMerkleRoot,
                    components.mapIndexed { index, component ->
                        ZKMerkleTree.computeComponentHash(
                            nonces[index],
                            component,
                            componentGroupLeafDigestService
                        )
                    })
            ) {
                "Visible components in group $groupIndex cannot be verified against their partial Merkle tree"
            }
        }
    }

    /**
     * Function that checks the whole filtered structure.
     * Force type checking on a structure that we obtained, so we don't sign more than expected.
     * Example: Oracle is implemented to check only for commands, if it gets an attachment and doesn't expect it - it can sign
     * over a transaction with the attachment that wasn't verified. Of course it depends on how you implement it, but else -> false
     * should solve a problem with possible later extensions to WireTransaction.
     * @param checkingFun function that performs type checking on the structure fields and provides verification logic accordingly.
     * @return false if no elements were matched on a structure or checkingFun returned false.
     */
    fun checkWithFun(checkingFun: (Any) -> Boolean): Boolean {
        val checkList = availableComponentGroups.flatten().map { checkingFun(it) }
        return (!checkList.isEmpty()) && checkList.all { it }
    }

    /**
     * Function that checks if all of the components in a particular group are visible.
     * This functionality is required on non-Validating Notaries to check that all inputs are visible.
     * It might also be applied in Oracles or any other entity requiring [Command] visibility, but because this method
     * cannot distinguish between related and unrelated to the signer [Command]s, one should use the
     * [checkCommandVisibility] method, which is specifically designed for [Command] visibility purposes.
     * The logic behind this algorithm is that we check that the root of the provided group partialMerkleTree matches with the
     * root of a fullMerkleTree if computed using all visible components.
     * Note that this method is usually called after or before [verify], to also ensure that the provided partial Merkle
     * tree corresponds to the correct leaf in the top Merkle tree.
     * @param componentGroupEnum the [ComponentGroupEnum] that corresponds to the componentGroup for which we require full component visibility.
     * @throws ComponentVisibilityException if not all of the components are visible or if the component group is not present in the [FilteredTransaction].
     */
    @Throws(ComponentVisibilityException::class)
    fun checkAllComponentsVisible(componentGroupEnum: ComponentGroupEnum) {
        val group = filteredComponentGroups.firstOrNull { it.groupIndex == componentGroupEnum.ordinal }
        if (group == null) {
            // If we don't receive elements of a particular component, check if its ordinal is bigger that the
            // groupHashes.size or if the group hash is allOnesHash,
            // to ensure there were indeed no elements in the original wire transaction.
            visibilityCheck(componentGroupEnum.ordinal >= groupHashes.size || groupHashes[componentGroupEnum.ordinal] == SecureHash.allOnesHash) {
                "Did not receive components for group ${componentGroupEnum.ordinal} and cannot verify they didn't exist in the original wire transaction"
            }
        } else {
            visibilityCheck(group.groupIndex < groupHashes.size) { "There is no matching component group hash for group ${group.groupIndex}" }
            val groupPartialRoot = groupHashes[group.groupIndex]
            val groupFullRoot = MerkleTree.getMerkleTree(group.components.mapIndexed { index, component ->
                ZKMerkleTree.computeComponentHash(
                    group.nonces[index],
                    component,
                    componentGroupLeafDigestService
                )
            }).hash
            visibilityCheck(groupPartialRoot == groupFullRoot) { "Some components for group ${group.groupIndex} are not visible" }
            // Verify the top level Merkle tree from groupHashes.
            visibilityCheck(MerkleTree.getMerkleTree(groupHashes).hash == id) {
                "Transaction is malformed. Top level Merkle tree cannot be verified against transaction's id"
            }
        }
    }

    /**
     * Function that checks if all of the commands that should be signed by the input public key are visible.
     * This functionality is required from Oracles to check that all of the commands they should sign are visible.
     * This algorithm uses the [ComponentGroupEnum.SIGNERS_GROUP] to count how many commands should be signed by the
     * input [PublicKey] and it then matches it with the size of received [commands].
     * Note that this method does not throw if there are no commands for this key to sign in the original [WireTransaction].
     * @param publicKey signer's [PublicKey]
     * @throws ComponentVisibilityException if not all of the related commands are visible.
     */
    @Throws(ComponentVisibilityException::class)
    fun checkCommandVisibility(publicKey: PublicKey) {
        val commandSigners =
            filteredComponentGroups.firstOrNull { it.groupIndex == ComponentGroupEnum.SIGNERS_GROUP.ordinal }
        val expectedNumOfCommands = expectedNumOfCommands(publicKey, commandSigners)
        val receivedForThisKeyNumOfCommands = commands.filter { publicKey in it.signers }.size
        visibilityCheck(expectedNumOfCommands == receivedForThisKeyNumOfCommands) {
            "$expectedNumOfCommands commands were expected, but received $receivedForThisKeyNumOfCommands"
        }
    }

    // Function to return number of expected commands to sign.
    private fun expectedNumOfCommands(publicKey: PublicKey, commandSigners: ComponentGroup?): Int {
        checkAllComponentsVisible(ComponentGroupEnum.SIGNERS_GROUP)
        if (commandSigners == null) return 0
        fun signersKeys(internalIndex: Int, opaqueBytes: OpaqueBytes): List<PublicKey> {
            try {
                return SerializedBytes<List<PublicKey>>(opaqueBytes.bytes).deserialize()
            } catch (e: Exception) {
                throw Exception("Malformed transaction, signers at index $internalIndex cannot be deserialised", e)
            }
        }

        return commandSigners.components
            .mapIndexed { internalIndex, opaqueBytes -> signersKeys(internalIndex, opaqueBytes) }
            .filter { signers -> publicKey in signers }.size
    }

    private inline fun verificationCheck(value: Boolean, lazyMessage: () -> Any) {
        if (!value) {
            val message = lazyMessage()
            throw FilteredTransactionVerificationException(id, message.toString())
        }
    }

    private inline fun visibilityCheck(value: Boolean, lazyMessage: () -> Any) {
        if (!value) {
            val message = lazyMessage()
            throw ComponentVisibilityException(id, message.toString())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is ZKVerifierTransaction) {
            return (this.id == other.id)
        }
        return false
    }

    override fun hashCode(): Int = id.hashCode()
}
