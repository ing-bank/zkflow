package com.ing.zkflow.annotations.corda

import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SecureHash
import kotlin.reflect.KClass

/*********************************************************
 * Corda specific annotations: [SecureHash]
 * Enables annotations:
 * val secureHash --> val secureHash: @MyHash SecureHash
 *********************************************************/

annotation class Algorithm(val digestAlgorithm: KClass<out DigestAlgorithm>)

/**
 * A predefined SHA256 hash type.
 */
@Target(AnnotationTarget.TYPE)
@Algorithm(SHA256DigestAlgorithm::class)
annotation class SHA256

/**
 * Placeholder [DigestAlgorithm] to define [SHA256] annotation class.
 */
class SHA256DigestAlgorithm : DigestAlgorithm {
    override val algorithm = SecureHash.SHA2_256
    override val digestLength = SecureHash.digestLengthFor(algorithm)

    override fun digest(bytes: ByteArray) = useSha256()
    override fun componentDigest(bytes: ByteArray) = useSha256()
    override fun nonceDigest(bytes: ByteArray) = useSha256()

    private fun useSha256(): Nothing {
        error("Use ${SecureHash.SHA256::class.qualifiedName} directly")
    }
}
