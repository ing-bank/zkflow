package com.ing.zkflow.common.zkp

import com.ing.zkflow.annotations.corda.Sha256
import com.ing.zkflow.crypto.IdentifyingDigestAlgorithm
import com.ing.zkflow.crypto.ZincDigestAlgorithm
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import kotlin.reflect.KClass

object ZKFlow {
    const val REQUIRED_PLATFORM_VERSION = 10 // Corda 4.8

    val DEFAULT_ZKFLOW_SIGNATURE_SCHEME = Crypto.EDDSA_ED25519_SHA512

    val DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT = SignatureAttachmentConstraint::class
    val DEFAULT_ZKFLOW_SIGNATURE_ATTACHMENT_CONSTRAINT_SIGNATURE_SCHEME = DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    val DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM = Sha256::class

    val DEFAULT_ZKFLOW_DIGEST_IDENTIFIER: IdentifyingDigestAlgorithm = ZincDigestAlgorithm()

    const val CIRCUITMANAGER_MAX_SETUP_WAIT_TIME_SECONDS = 10000 // seconds

    fun requireSupportedContractAttachmentConstraint(constraint: KClass<out AttachmentConstraint>) {
        /**
         * Since Corda 4, the SignatureAttachmentConstraint is the recommended constraint. For simplicity, we support only this
         * until we are forced to do otherwise.
         */
        require(constraint == DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT) {
            "Unsupported contract attachment constraint: $constraint. Only $DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT is supported."
        }
    }

    fun requireSupportedSignatureScheme(scheme: SignatureScheme) {
        /**
         * Initially, we only support Crypto.EDDSA_ED25519_SHA512.
         * Later, we may support all scheme supported by Corda: `require(participantSignatureScheme in supportedSignatureSchemes())`
         */
        require(scheme == Crypto.EDDSA_ED25519_SHA512) {
            "Unsupported signature scheme: ${scheme.schemeCodeName}. Only ${Crypto.EDDSA_ED25519_SHA512.schemeCodeName} is supported."
        }
    }

    fun requireSupportedDigestService(digestIdentifier: IdentifyingDigestAlgorithm) {
        /**
         * Initially, we only support ZincDigestAlgorithm. This may become more flexible later, depending on use case needs
         */
        require(digestIdentifier == DEFAULT_ZKFLOW_DIGEST_IDENTIFIER) {
            "Unsupported digest: $digestIdentifier. Only $DEFAULT_ZKFLOW_DIGEST_IDENTIFIER is supported."
        }
    }
}
