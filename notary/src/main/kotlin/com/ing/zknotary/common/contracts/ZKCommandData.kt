package com.ing.zknotary.common.contracts

import com.ing.zknotary.common.zkp.CircuitMetaData
import com.ing.zknotary.common.zkp.ZKCommandMetadata
import com.ing.zknotary.common.zkp.ZKTransactionMetadata
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties

/**
 * Any command that implements this interface is expected to be the first command in a ZKP transaction
 * it is part of. Its [ZKTransactionMetadata] will be inspected to resolve all transaction metadata
 */
interface ZKTransactionMetadataCommandData : ZKCommandData {
    val transactionMetadata: ZKTransactionMetadata
}

interface ZKCommandData : CommandData {
    val circuit: CircuitMetaData
    val metadata: ZKCommandMetadata
}

fun <T : CommandData> CommandWithParties<T>.toZKCommand(): Command<ZKCommandData> {
    require(value is ZKCommandData) { "CommandData must implement ZKCommandData" }
    return Command(value as ZKCommandData, signers)
}
