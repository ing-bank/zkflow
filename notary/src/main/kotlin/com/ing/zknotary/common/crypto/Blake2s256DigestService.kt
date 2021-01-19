package com.ing.zknotary.common.crypto

import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.internal.DigestAlgorithmFactory
import org.bouncycastle.crypto.digests.Blake2sDigest

class Blake2s256DigestAlgorithm : DigestAlgorithm {
    override val algorithm = "BLAKE2S256"

    override val digestLength = 32

    override fun digest(bytes: ByteArray): ByteArray {
        val blake2s256 = Blake2sDigest(null, digestLength, null, "12345678".toByteArray())
        blake2s256.reset()
        blake2s256.update(bytes, 0, bytes.size)
        val hash = ByteArray(digestLength)
        blake2s256.doFinal(hash, 0)
        return hash
    }

    override fun preImageResistantDigest(bytes: ByteArray): ByteArray = digest(bytes)
}

val blake2sAlgorithm = DigestAlgorithmFactory.registerClass(PedersenDigestAlgorithm::class.java.name)
val DigestService.Companion.blake2s256: DigestService by lazy { DigestService(blake2sAlgorithm) }
