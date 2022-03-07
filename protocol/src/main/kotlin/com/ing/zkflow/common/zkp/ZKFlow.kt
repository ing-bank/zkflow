package com.ing.zkflow.common.zkp

import com.ing.zkflow.annotations.corda.Sha256
import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.crypto.IdentifyingDigestAlgorithm
import com.ing.zkflow.crypto.ZincDigestAlgorithm
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme

object ZKFlow {
    const val REQUIRED_PLATFORM_VERSION = 10 // Corda 4.8

    val DEFAULT_ZKFLOW_SIGNATURE_SCHEME = Crypto.EDDSA_ED25519_SHA512
    val DEFAULT_ZKFLOW_NOTARY_SIGNATURE_SCHEME = DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    val DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT = SignatureAttachmentConstraint::class
    val DEFAULT_ZKFLOW_SIGNATURE_ATTACHMENT_CONSTRAINT_SIGNATURE_SCHEME = DEFAULT_ZKFLOW_SIGNATURE_SCHEME
    val DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT_TYPE =
        ZKAttachmentConstraintType.SignatureAttachmentConstraintType(DEFAULT_ZKFLOW_SIGNATURE_ATTACHMENT_CONSTRAINT_SIGNATURE_SCHEME)

    val DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM = Sha256::class

    val DEFAULT_ZKFLOW_DIGEST_IDENTIFIER: IdentifyingDigestAlgorithm = ZincDigestAlgorithm()

    const val DEFAULT_SERIALIZATION_SCHEME_ID = BFLSerializationScheme.SCHEME_ID

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
