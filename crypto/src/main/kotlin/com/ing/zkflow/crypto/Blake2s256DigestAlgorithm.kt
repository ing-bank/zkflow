package com.ing.zkflow.crypto

import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.internal.DigestAlgorithmFactory
import org.bouncycastle.crypto.digests.Blake2sDigest

class Blake2s256DigestAlgorithm : DigestAlgorithm {
    override val algorithm = "BLAKE2S256"

    override val digestLength = 32

    override fun digest(bytes: ByteArray): ByteArray {
        val digest = Blake2sDigest(null, digestLength, null, "12345678".toByteArray())
        digest.update(bytes, 0, bytes.size)
        val hash = ByteArray(digestLength)
        digest.doFinal(hash, 0)
        return hash
    }

    override fun componentDigest(bytes: ByteArray): ByteArray = digest(bytes)
    override fun nonceDigest(bytes: ByteArray): ByteArray = digest(bytes)
}

val blake2sAlgorithm = DigestAlgorithmFactory.registerClass(Blake2s256DigestAlgorithm::class.java.name)
val DigestService.Companion.blake2s256: DigestService by lazy { DigestService(blake2sAlgorithm) }
val SecureHash.Companion.BLAKE2S256: String
    get() = blake2sAlgorithm
