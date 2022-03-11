package com.ing.zkflow.common.network

import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.util.requireInstanceOf
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme
import kotlin.reflect.KClass

/**
 * This only implements AlwaysAcceptAttachmentConstraint, SignatureAttachmentConstraint and HashAttachmentConstraint.
 * The reason is that we currently only support SignatureAttachmentConstraint for production use and need to support
 * AlwaysAcceptAttachmentConstraint and HashAttachmentConstraint for testing only.
 */
sealed class ZKAttachmentConstraintType private constructor(val kClass: KClass<out AttachmentConstraint>) {
    abstract fun validate(attachmentConstraint: AttachmentConstraint)

    object AlwaysAcceptAttachmentConstraintType : ZKAttachmentConstraintType(AlwaysAcceptAttachmentConstraint::class) {
        override fun validate(attachmentConstraint: AttachmentConstraint) {
            attachmentConstraint.requireInstanceOf<AlwaysAcceptAttachmentConstraint>()
        }
    }

    class SignatureAttachmentConstraintType(val signatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_ATTACHMENT_CONSTRAINT_SIGNATURE_SCHEME) :
        ZKAttachmentConstraintType(SignatureAttachmentConstraint::class) {
        override fun validate(attachmentConstraint: AttachmentConstraint) {
            attachmentConstraint.requireInstanceOf<SignatureAttachmentConstraint>().also {
                require(Crypto.findSignatureScheme(it.key) == signatureScheme) { "The algorithm for the PublicKey of SignatureAttachmentConstraint is ${it.key.algorithm}, expected ${signatureScheme.algorithmName}" }
            }
        }
    }

    class HashAttachmentConstraintType(val digestAlgorithm: DigestAlgorithm = ZKFlow.DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM) :
        ZKAttachmentConstraintType(HashAttachmentConstraint::class) {

        override fun validate(attachmentConstraint: AttachmentConstraint) {
            attachmentConstraint.requireInstanceOf<HashAttachmentConstraint>().also {
                // TODO: to discus: should HashAttachmentConstraintType contain a real required SecureHash algorithm?
                // Then we can compare actual required algo. Now we only can compare that the runtime length matches the network expected length
                require(it.attachmentId.size == digestAlgorithm.digestLength) { "Expected a digest of size ${digestAlgorithm.digestLength}, found ${it.attachmentId.size}" }
            }
        }
    }
}
