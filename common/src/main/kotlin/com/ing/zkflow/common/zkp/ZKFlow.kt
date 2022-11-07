package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.crypto.Blake2s256DigestAlgorithm
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignatureScheme
import net.corda.core.crypto.internal.DigestAlgorithmFactory

object ZKFlow {
    const val REQUIRED_PLATFORM_VERSION = 10 // Corda 4.8

    val DEFAULT_ZKFLOW_SIGNATURE_SCHEME = Crypto.EDDSA_ED25519_SHA512
    val DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME = DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    val DEFAULT_ZKFLOW_SIGNATURE_ATTACHMENT_CONSTRAINT_SIGNATURE_SCHEME = DEFAULT_ZKFLOW_SIGNATURE_SCHEME
    val DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT_TYPE =
        ZKAttachmentConstraintType.SignatureAttachmentConstraintType(DEFAULT_ZKFLOW_SIGNATURE_ATTACHMENT_CONSTRAINT_SIGNATURE_SCHEME)

    val DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM = DigestAlgorithmFactory.create(SecureHash.SHA2_256)

    val DEFAULT_ZKFLOW_DIGEST_IDENTIFIER: DigestAlgorithm = Blake2s256DigestAlgorithm()

    const val CIRCUITMANAGER_MAX_SETUP_WAIT_TIME_SECONDS = 10000 // seconds

    fun requireSupportedSignatureScheme(scheme: SignatureScheme) {
        /**
         * Initially, we only support Crypto.EDDSA_ED25519_SHA512.
         * Later, we may support all scheme supported by Corda: `require(participantSignatureScheme in supportedSignatureSchemes())`
         */
        require(scheme == Crypto.EDDSA_ED25519_SHA512) {
            "Unsupported signature scheme: ${scheme.schemeCodeName}. Only ${Crypto.EDDSA_ED25519_SHA512.schemeCodeName} is supported."
        }
    }
}
