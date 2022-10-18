package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.transactions.TraversableTransaction

fun TraversableTransaction.serializedComponentBytesFor(
    groupEnum: ComponentGroupEnum,
    metadata: ResolvedZKCommandMetadata
): List<ByteArray> {
    return componentGroups.singleOrNull { it.groupIndex == groupEnum.ordinal }?.components
        ?.filterIndexed { index, _ -> metadata.isVisibleInWitness(groupEnum.ordinal, index) }
        ?.map { it.copyBytes() } ?: emptyList()
}
