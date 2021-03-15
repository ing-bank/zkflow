package com.ing.zknotary.common.crypto

import com.ing.dlt.zkkrypto.ecc.pedersenhash.PedersenHash
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.internal.DigestAlgorithmFactory
import org.bouncycastle.crypto.digests.Blake2sDigest

/**
 * This is in fact a combination of 2 algorithms: Blake2s and Pedersen.
 * But because of Corda's hash agility architecture they should be combined in a single algorithm instance
 * in order to be able to use different hash algorithms for leaves and nodes of a Merkle tree.
 *
 * At the moment Blake and Pedersen have the same length 32, but this theoretically can change if we use different
 * elliptic curve for Pedersen. In this case we can either switch from Blake2s to Blake2b that allows for longer output,
 * or continue to use blake2s and just pad it with trailing zeroes according to Pedersen algorithm. First way will hurt
 * performance, so probably better to go with second option (although need to double-check security wouldn't suffer)
 */
class ZincDigestAlgorithm : DigestAlgorithm {
    override val algorithm = "ZINC"

    // This is Blake's length, can be different for Pedersen
    override val digestLength = 32

    private val pedersen = PedersenHash.zinc()

    /**
     * This method is called for nodes hashing
     */
    override fun digest(bytes: ByteArray): ByteArray = pedersen.hash(bytes)

    /**
     * This method is called for leaves (components) hashing
     */
    override fun componentDigest(bytes: ByteArray): ByteArray {
        val digest = Blake2sDigest(null, digestLength, null, "12345678".toByteArray())
        digest.update(bytes, 0, bytes.size)
        val hash = ByteArray(digestLength)
        digest.doFinal(hash, 0)
        return hash
    }

    /**
     * We use Blake for nonces as well. TODO double-check
     */
    override fun nonceDigest(bytes: ByteArray): ByteArray = componentDigest(bytes)
}

val zincAlgorithm = DigestAlgorithmFactory.registerClass(ZincDigestAlgorithm::class.java.name)
val DigestService.Companion.zinc: DigestService by lazy { DigestService(zincAlgorithm) }
val SecureHash.Companion.ZINC: String
    get() = zincAlgorithm
