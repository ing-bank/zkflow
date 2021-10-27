package com.ing.zkflow.common.contracts

import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.ZKTransactionMetadata
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import java.util.ServiceLoader

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

fun loadResolvedTransactionMetadata(): List<ResolvedZKTransactionMetadata> {
    return ServiceLoader.load(ZKTransactionMetadataCommandData::class.java)
        .map {
            it.transactionMetadata
        }
}
