package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.zkp.Proof
import net.corda.core.KeepForDJVM
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TraversableTransaction

@KeepForDJVM
@CordaSerializable
class ZKFilteredTransaction(val proof: Proof, private val ftx: FilteredTransaction) :
    TraversableTransaction(ftx.filteredComponentGroups) {
    override val id: SecureHash = ftx.id

    fun verify() {
        // Check that the merkle tree of the ftx is correct
        ftx.verify()

        // If the merkle tree is correct, confirm that the required components are visible
        ftx.checkAllComponentsVisible(ComponentGroupEnum.INPUTS_GROUP)
        ftx.checkAllComponentsVisible(ComponentGroupEnum.TIMEWINDOW_GROUP)
        ftx.checkAllComponentsVisible(ComponentGroupEnum.REFERENCES_GROUP)
        ftx.checkAllComponentsVisible(ComponentGroupEnum.PARAMETERS_GROUP)
    }
}
