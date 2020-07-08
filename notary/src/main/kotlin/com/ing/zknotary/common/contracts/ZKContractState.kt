package com.ing.zknotary.common.contracts

import com.ing.zknotary.common.zkp.Fingerprintable
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.secureRandomBytes
import java.nio.ByteBuffer

interface ZKContractState : Fingerprintable, ContractState {
    val nonce: StateNonce
        get() = StateNonce()
}

interface ZKCommandData : Fingerprintable, CommandData {
    val id: Int

    override val fingerprint: ByteArray
        get() = ByteBuffer.allocate(4).putInt(id).array()
}

class StateNonce(val bytes: ByteArray) : Fingerprintable {
    override val fingerprint: ByteArray = bytes

    @DeleteForDJVM
    constructor() : this(secureRandomBytes(32))

    init {
        require(bytes.size == 32) { "State nonce should be 32 bytes." }
    }
}
