package com.ing.zkflow

import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.commandMetadata
import com.ing.zkflow.common.transactions.hasZKCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import net.corda.core.contracts.ContractState
import net.corda.core.transactions.BaseTransaction
import net.corda.core.transactions.TraversableTransaction

const val TX_CONTAINS_NO_COMMANDS_WITH_METADATA = "This transaction does not contain any commands with metadata"

fun TraversableTransaction.zkTransactionMetadata(): ResolvedZKTransactionMetadata {
    return if (this.hasZKCommandData) com.ing.zkflow.common.transactions.zkTransactionMetadata(
        this.commandMetadata,
        ClassLoader.getSystemClassLoader()
    ) else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)
}

fun TraversableTransaction.zkTransactionMetadataOrNull(): ResolvedZKTransactionMetadata? =
    if (this.hasZKCommandData) com.ing.zkflow.common.transactions.zkTransactionMetadata(
        this.commandMetadata,
        ClassLoader.getSystemClassLoader()
    ) else null

fun ZKTransactionBuilder.zkTransactionMetadata(): ResolvedZKTransactionMetadata {
    return if (this.hasZKCommandData) {
        com.ing.zkflow.common.transactions.zkTransactionMetadata(
            this.commandMetadata,
            ClassLoader.getSystemClassLoader()
        )
    } else error(TX_CONTAINS_NO_COMMANDS_WITH_METADATA)
}

fun BaseTransaction.confirmExpectedVisibilityAfterFiltering(visible: List<ContractState>, invisible: List<ContractState>) {
    visible.forEach { outputStates shouldContain it }
    invisible.forEach { outputStates shouldNotContain it }
}
