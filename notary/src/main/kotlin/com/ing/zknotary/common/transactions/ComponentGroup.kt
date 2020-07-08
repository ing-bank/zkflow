package com.ing.zknotary.common.transactions

import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.transactions.ComponentGroup
import net.corda.core.utilities.OpaqueBytes

fun MutableList<ComponentGroup>.addGroup(
    groupEnum: ComponentGroupEnum,
    data: List<ByteArray?>
) {
    val filtered = data.filterNotNull()
    if (filtered.isNotEmpty()) {
        this.add(ComponentGroup(groupEnum.ordinal, filtered.map(::OpaqueBytes)))
    }
}

fun MutableList<ComponentGroup>.addGroups(
    groups: Map<ComponentGroupEnum, List<ByteArray?>>
) = groups.forEach { (groupEnum, data) -> this.addGroup(groupEnum, data) }
