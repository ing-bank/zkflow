package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.ZKProverTransaction

interface Witness {
    val transaction: ZKProverTransaction
    val signatures: List<ByteArray>
}
