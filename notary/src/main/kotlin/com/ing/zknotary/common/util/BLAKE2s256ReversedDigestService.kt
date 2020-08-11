package com.ing.zknotary.common.util

import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash

object BLAKE2s256ReversedDigestService : DigestService by BLAKE2s256DigestService {
    override fun hash(bytes: ByteArray) =
        SecureHash.BLAKE2s256(BLAKE2s256DigestService.hash(bytes.reverseBits()).bytes.reverseBits())

    private fun ByteArray.reverseBits() = map { it.reverseBits() }.toByteArray()

    private fun Byte.reverseBits(): Byte {
        var x = this.toInt()
        var y: Byte = 0
        for (position in 8 - 1 downTo 0) {
            y = (y + (x and 1 shl position).toByte()).toByte()
            x = (x shr 1)
        }
        return y
    }
}
