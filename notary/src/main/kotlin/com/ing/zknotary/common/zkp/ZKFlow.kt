package com.ing.zknotary.common.zkp

import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import kotlin.reflect.KClass

object ZKFlow {
    val DEFAULT_ZKFLOW_SIGNATURE_SCHEME = Crypto.EDDSA_ED25519_SHA512
    val DEFAULT_ZKFLOW_CONTRACT_ATTACHMENT_CONSTRAINT = SignatureAttachmentConstraint::class

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
}
