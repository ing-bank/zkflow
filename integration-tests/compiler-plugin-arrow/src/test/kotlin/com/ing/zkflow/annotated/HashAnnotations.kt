package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.Algorithm
import com.ing.zkflow.annotations.corda.SHA256
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SecureHash

@ZKP
data class HashAnnotations(
    val sha256: @SHA256 SecureHash = SecureHash.zeroHash,
    val fancyHash: @FancyHash SecureHash = SecureHash.HASH("FancyHash", ByteArray(FancyDigestAlgorithm.DIGEST_LENGTH) { 0 })
)

@Target(AnnotationTarget.TYPE)
@Algorithm(FancyDigestAlgorithm::class)
annotation class FancyHash

class FancyDigestAlgorithm : DigestAlgorithm {
    override val algorithm = "FancyHash"
    override val digestLength = DIGEST_LENGTH

    override fun digest(bytes: ByteArray) = placeholder()
    override fun componentDigest(bytes: ByteArray) = placeholder()
    override fun nonceDigest(bytes: ByteArray) = placeholder()

    private fun placeholder(): Nothing {
        error("`${this::class.qualifiedName}` is a placeholder DigestAlgorithm only used in tests.")
    }

    companion object {
        const val DIGEST_LENGTH = 8
    }
}
