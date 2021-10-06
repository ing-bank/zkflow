package com.ing.zknotary.common.contracts

import com.ing.zknotary.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zknotary.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zknotary.common.zkp.metadata.ZKTransactionMetadata
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties

/**
 * Any command that implements this interface is expected to be the first command in a ZKP transaction
 * it is part of. Its [ZKTransactionMetadata] will be inspected to resolve all transaction metadata
 */
interface ZKTransactionMetadataCommandData : ZKCommandData {
    val transactionMetadata: ResolvedZKTransactionMetadata
}

interface ZKCommandData : CommandData {
    companion object {
        const val METADATA_FIELD_NAME: String = "metadata"
    }

    val metadata: ResolvedZKCommandMetadata
}

fun <T : CommandData> CommandWithParties<T>.toZKCommand(): Command<ZKCommandData> {
    require(value is ZKCommandData) { "CommandData must implement ZKCommandData" }
    return Command(value as ZKCommandData, signers)
}
