package com.ing.zknotary.common.zkp

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import java.nio.ByteBuffer
import java.security.PublicKey
import java.time.Instant

/**
 * Classes implementing this interface provide a deterministic representation of its content.
 *
 * This representation should be identical for objects with the same contents.
 * This should also be true when a new object is instantiated with these same values at a later point in time.
 * e.g. when reconstructed from a database.
 *
 * An example usage is to use the fingerprint as input for the leaf hashes when building a Merkle tree.
 */
interface Fingerprintable {
    val fingerprint: ByteArray
}

val Int.fingerprint: ByteArray
    get() = ByteBuffer.allocate(4).putInt(this).array()

val PublicKey.fingerprint: ByteArray
    get() = this.encoded

val <T> TransactionState<T>.fingerprint: ByteArray
    where T : ContractState, T : Fingerprintable
    get() = data.fingerprint + notary.owningKey.fingerprint

val AbstractParty.fingerprint: ByteArray
    get() = this.owningKey.fingerprint

val SecureHash.fingerprint: ByteArray
    get() = this.bytes

val Instant.fingerprint: ByteArray
    get() = ByteBuffer.allocate(8).putLong(epochSecond).array() +
        ByteBuffer.allocate(4).putInt(nano).array()

val TimeWindow.fingerprint: ByteArray
    get() = (fromTime?.fingerprint ?: ByteArray(12) { 0 }) +
        (untilTime?.fingerprint ?: ByteArray(12) { 0 })
