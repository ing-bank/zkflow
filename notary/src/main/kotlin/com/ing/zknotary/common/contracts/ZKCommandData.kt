package com.ing.zknotary.common.contracts

import com.ing.zknotary.common.zkp.CircuitMetaData
import com.ing.zknotary.common.zkp.ZKCommandMetadata
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties

interface ZKCommandData : CommandData {
    val circuit: CircuitMetaData
    val metadata: ZKCommandMetadata
}

fun <T : CommandData> CommandWithParties<T>.toZKCommand(): Command<ZKCommandData> {
    require(value is ZKCommandData) { "CommandData must implement ZKCommandData" }
    return Command(value as ZKCommandData, signers)
}
fun Command<*>.toZKCommand(): Command<ZKCommandData> {
    require(value is ZKCommandData) { "CommandData must implement ZKCommandData" }
    return Command(value as ZKCommandData, signers)
}
