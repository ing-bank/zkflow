package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.ZKProverTransaction

data class Witness(
    val ptx: ZKProverTransaction,
    val sigs: List<ByteArray>
)
