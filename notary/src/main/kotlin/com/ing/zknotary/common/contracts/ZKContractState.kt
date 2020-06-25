package com.ing.zknotary.common.contracts

import net.corda.core.DeleteForDJVM
import net.corda.core.crypto.secureRandomBytes

interface ZKContractState {
    val nonce: StateNonce
        get() = StateNonce()
}

class StateNonce(bytes: ByteArray) {
    @DeleteForDJVM
    constructor() : this(secureRandomBytes(32))

    init {
        require(bytes.size == 32) { "State nonce should be 32 bytes." }
    }
}
