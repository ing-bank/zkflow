package com.ing.zkflow.crypto

import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.internal.DigestAlgorithmFactory

/**
 * This is in fact a combination of 2 algorithms: Blake2s and Pedersen.
 *
 * Because of Corda's hash agility architecture they should be combined in a single algorithm instance
 * in order to be able to use different hash algorithms for leaves and nodes of a Merkle tree.
 *
 */
class ZincDigestAlgorithm : IdentifyingDigestAlgorithm {
    override val algorithm = "Z" // Keep this as short as possible, since it is part of serialized data

    override val digestAlgorithm: DigestAlgorithm = ZincPedersenDigestAlgorithm()
    override val componentDigestAlgorithm: DigestAlgorithm = Blake2s256DigestAlgorithm()
    override val nonceDigestAlgorithm: DigestAlgorithm = componentDigestAlgorithm

    init {
        val allDigestLengtsAreEqual = digestAlgorithm.digestLength == componentDigestAlgorithm.digestLength &&
            digestAlgorithm.digestLength == nonceDigestAlgorithm.digestLength

        /*
         * At the moment Blake and Pedersen have the same length 32, but this theoretically can change if we use different
         * elliptic curve for Pedersen. In this case we can either switch from Blake2s to Blake2b that allows for longer output,
         * or continue to use blake2s and just pad it with trailing zeroes according to Pedersen algorithm. First way will hurt
         * performance, so probably better to go with second option (although need to double-check security wouldn't suffer)
         */
        require(allDigestLengtsAreEqual) { "All digest lengths should be equal" }
    }

    override val digestLength = digestAlgorithm.digestLength

    override fun digest(bytes: ByteArray): ByteArray = digestAlgorithm.digest(bytes)
    override fun componentDigest(bytes: ByteArray): ByteArray = componentDigestAlgorithm.digest(bytes)
    override fun nonceDigest(bytes: ByteArray): ByteArray = componentDigest(bytes)
}

val zincAlgorithm = DigestAlgorithmFactory.registerClass(ZincDigestAlgorithm::class.java.name)
val DigestService.Companion.zinc: DigestService by lazy { DigestService(zincAlgorithm) }
val SecureHash.Companion.ZINC: String
    get() = zincAlgorithm
