// package com.ing.zknotary.common.transactions
//
// import com.ing.zknotary.common.dactyloscopy.Dactyloscopist
// import net.corda.core.contracts.ComponentGroupEnum
// import net.corda.core.crypto.SecureHash
// import net.corda.core.transactions.ComponentGroup
//
// class ZKPartialMerkleTree(
//    vtx: ZKVerifierTransaction
// ) : AbstractZKMerkleTree(
//    createComponentGroups(vtx),
//    vtx.componentGroupLeafDigestService,
//    vtx.nodeDigestService
// ) {
//
//    companion object {
//        /**
//         * In this case, we don't include a group for outputs, as we will add the output
//         * hashes from the constructor parameter directly when calculating the component hashes
//         * for the leaves below in [componentHashes]
//         *
//         * We also leave out the groups for commands, signers, attachments and we only use the group hash
//         */
//        private fun createComponentGroups(vtx: ZKVerifierTransaction): List<ComponentGroup> =
//            mutableListOf<ComponentGroup>().apply {
//                addGroups(
//                    mapOf(
//                        ComponentGroupEnum.INPUTS_GROUP to
//                            vtx.padded.inputs().map { Dactyloscopist.identify(it.content) },
//                        ComponentGroupEnum.REFERENCES_GROUP to
//                            vtx.padded.references().map { Dactyloscopist.identify(it.content) },
//                        ComponentGroupEnum.NOTARY_GROUP to
//                            listOf(Dactyloscopist.identify(vtx.notary)),
//                        ComponentGroupEnum.TIMEWINDOW_GROUP to
//                            listOf(Dactyloscopist.identify(vtx.padded.timeWindow().content)),
//                        ComponentGroupEnum.SIGNERS_GROUP to
//                            vtx.padded.signers().map { Dactyloscopist.identify(it.content) },
//                        ComponentGroupEnum.PARAMETERS_GROUP to
//                            listOf(Dactyloscopist.identify(vtx.padded.networkParametersHash().content))
//                    )
//                )
//            }
//    }
//
//    /**
//     * For groups where we don't have a groupsMerkleRoot, we take the grouphash from the ZKProverTransaction.
//     * If that is not present, we fail hard. All groups should always be present in the ZKProverTransaction.
//     * We don't accept unknown groups.
//     */
//    override val groupHashes: List<SecureHash> by lazy {
//        ComponentGroupEnum.values().map {
//            groupsMerkleRoots[it.ordinal] ?: vtx.groupHashes[it.ordinal]
//        }
//    }
//
//    override val componentNonces: Map<Int, List<SecureHash>> = vtx.componentNonces
//
//    // Override because we need to include the output hashes from the ProverTransaction unchanged
//    override val componentHashes: Map<Int, List<SecureHash>> by lazy {
//        val hashes = super.componentHashes.toMutableMap()
//
//        // Include the pre-calculated leaf hashes for the outputs as provided from the ProverTransaction
//        if (vtx.outputHashes.isNotEmpty()) hashes[ComponentGroupEnum.OUTPUTS_GROUP.ordinal] = vtx.outputHashes
//
//        hashes
//    }
// }
