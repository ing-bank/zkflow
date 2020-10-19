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
import java.util.AbstractList

/**
 * PLEASE READ before extending new types with the fingerprinting functionality.
 *
 * Extending types with the fingerprinting functionality is easy, but not always risk-free.
 * Consider implication before amending this file.
 *
 * Java/Kotlin allows for complex class hierarchies, thus the type implementing or extending
 * from any of the types extended here will inherit the fingerprinting functionality from the parent.
 * Such behavior is not guaranteed to produce correct fingerprint for the children classes.
 * It may happen that an implementing/extending class contains extra fields which must be accounted in
 * its fingerprint, but these fields will be skipped and the parent fingerprinting functionality
 * will be used. This argument demands higher attention to what types are extended with
 * the fingerprinting functionality.
 *
 * Types that are neither implementable nor extendable are SAFE:
 * 1. Types marked with the keyword `final` in Java,
 * 2. *not* marked with the keyword `open` in Kotlin,
 * 3. *not* marked with the keyword `abstract` in either language
 *
 * Interfaces and non final classes (`open` in Kotlin) are UNSAFE and must be extended with great care.
 */

/*
 * Safe classes.
 */
fun Int.fingerprint(): ByteArray =
    ByteBuffer.allocate(4).putInt(this).array()

fun ByteArray.fingerprint(): ByteArray =
    this

fun Instant.fingerprint(): ByteArray =
    ByteBuffer.allocate(8).putLong(epochSecond).array() +
        ByteBuffer.allocate(4).putInt(nano).array()

fun StateRef.fingerprint(): ByteArray =
    txhash.fingerprint() + index.fingerprint()

// FIXME: This is now ignoring some of the important fields of a TransactionState.
fun <T> TransactionState<T>.fingerprint(): ByteArray
    where T : ContractState, T : Fingerprintable =
    data.fingerprint() + notary.owningKey.fingerprint()

/*
 * Unsafe types: interfaces and extendable classes
 */

fun PublicKey.fingerprint(): ByteArray =
    this.encoded

fun AbstractParty.fingerprint(): ByteArray =
    this.owningKey.fingerprint()

fun SecureHash.fingerprint(): ByteArray =
    this.bytes

fun TimeWindow.fingerprint(): ByteArray =
    (fromTime?.fingerprint() ?: ByteArray(12) { 0 }) +
        (untilTime?.fingerprint() ?: ByteArray(12) { 0 })

fun AbstractList<Any>.fingerprint(): ByteArray {
    val dactyloscopist = Dactyloscopist()
    return this.fold(ByteArray(0)) { acc, element ->
        acc + dactyloscopist.identify(element)
    }
}

/*
 * Annotation to skip fields from including into the fingerprint.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class NonFingerprintable
