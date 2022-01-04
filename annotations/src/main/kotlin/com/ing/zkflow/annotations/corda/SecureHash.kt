package com.ing.zkflow.annotations.corda

/*********************************************************
 * Corda specific annotations: [SecureHash]
 * Enables annotations:
 * val secureHash --> val secureHash: @MyHash SecureHash
 *********************************************************/

/**
 * Allow user to define own hash types.
 * `size` is the size of a byte array representation of the hash.
 */
annotation class HashSize(val size: Int)

/**
 * A predefined SHA256 hash type.
 */
@Target(AnnotationTarget.TYPE)
@Suppress("MagicNumber")
@HashSize(32)
annotation class Sha256
