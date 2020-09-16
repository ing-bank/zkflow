package com.ing.zknotary.common.dactyloscopy

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import java.nio.ByteBuffer
import java.security.PublicKey
import java.time.Instant

/*
 * Corda classes
 */
fun StateRef.fingerprint(): ByteArray =
    txhash.bytes + index.fingerprint()

// FIXME: This is now ignoring some of the important fields of a TransactionState.
fun <T> TransactionState<T>.fingerprint(): ByteArray
    where T : ContractState, T : Fingerprintable =
    data.fingerprint() + notary.owningKey.fingerprint()

fun AbstractParty.fingerprint(): ByteArray =
    this.owningKey.fingerprint()

fun SecureHash.fingerprint(): ByteArray =
    this.bytes

fun TimeWindow.fingerprint(): ByteArray =
    (fromTime?.fingerprint() ?: ByteArray(12) { 0 }) +
        (untilTime?.fingerprint() ?: ByteArray(12) { 0 })

/*
 * Core Java/Kotlin classes
 */
fun Int.fingerprint(): ByteArray =
    ByteBuffer.allocate(4).putInt(this).array()

fun ByteArray.fingerprint(): ByteArray =
    this

fun PublicKey.fingerprint(): ByteArray =
    this.encoded

fun Instant.fingerprint(): ByteArray =
    ByteBuffer.allocate(8).putLong(epochSecond).array() +
        ByteBuffer.allocate(4).putInt(nano).array()
