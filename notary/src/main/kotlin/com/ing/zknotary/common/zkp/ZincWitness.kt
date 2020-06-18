package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.ZKProverTransaction

data class ZincWitness(override val transaction: ZKProverTransaction, override val signatures: List<ByteArray>) : Witness
