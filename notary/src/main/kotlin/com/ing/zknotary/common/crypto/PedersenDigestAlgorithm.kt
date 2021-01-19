package com.ing.zknotary.common.crypto

import com.ing.dlt.zkkrypto.ecc.pedersenhash.PedersenHash
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.internal.DigestAlgorithmFactory

class PedersenDigestAlgorithm : DigestAlgorithm {
    private val pedersen = PedersenHash.zinc()

    override val algorithm = "PEDERSEN"

    override val digestLength: Int by lazy { pedersen.hashLength }

    override fun digest(bytes: ByteArray): ByteArray = pedersen.hash(bytes)

    override fun preImageResistantDigest(bytes: ByteArray): ByteArray = digest(bytes)
}

val pedersenAlgorithm = DigestAlgorithmFactory.registerClass(PedersenDigestAlgorithm::class.java.name)
val DigestService.Companion.pedersen: DigestService by lazy { DigestService(pedersenAlgorithm) }
