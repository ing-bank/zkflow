package com.ing.zkflow.crypto

import net.corda.core.crypto.DigestAlgorithm

/**
 * IdentifyingDigestAlgorithm indicates for each type of digest function in its parent interface, which underlying digest algorithm is used.
 */
interface IdentifyingDigestAlgorithm : DigestAlgorithm {
    /**
     * Indicates the underlying DigestAlgorithm for `digest(bytes: ByteArray)`
     */
    val digestAlgorithm: DigestAlgorithm

    /**
     * Indicates the underlying DigestAlgorithm for `componentDigest(bytes: ByteArray)`
     */
    val componentDigestAlgorithm: DigestAlgorithm

    /**
     * Indicates the underlying DigestAlgorithm used for `nonceDigest(bytes: ByteArray)`
     */
    val nonceDigestAlgorithm: DigestAlgorithm
}
