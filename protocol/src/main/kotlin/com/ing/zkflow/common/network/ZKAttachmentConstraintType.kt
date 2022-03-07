package com.ing.zkflow.common.network

import com.ing.zkflow.annotations.corda.HashSize
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.util.requireInstanceOf
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
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

    class HashAttachmentConstraintType(val algorithm: KClass<out Annotation> = ZKFlow.DEFAULT_ZKFLOW_HASH_ATTACHMENT_HASHING_ALGORITHM) :
        ZKAttachmentConstraintType(HashAttachmentConstraint::class) {
        val digestLength: Int by lazy { getDigestLengthFromAnnotation(algorithm) }

        override fun validate(attachmentConstraint: AttachmentConstraint) {
            attachmentConstraint.requireInstanceOf<HashAttachmentConstraint>().also {
                // TODO: to discus: should HashAttachmentConstraintType contain a real required SecureHash algorithm?
                // Then we can compare actual required algo. Now we only can compare that the runtime length matches the network expected length
                require(it.attachmentId.size == digestLength) { "Expected a digest of size $digestLength, found ${it.attachmentId.size}" }
            }
        }

        private fun getDigestLengthFromAnnotation(algorithm: KClass<out Annotation>): Int {
            val hashSizeAnnotations = algorithm.annotations.filterIsInstance<HashSize>()
            when (hashSizeAnnotations.size) {
                0 -> error("Hash class `${algorithm.qualifiedName}` must have a `${HashSize::class.qualifiedName}` annotation")
                1 -> return hashSizeAnnotations.single().size
                else -> error("Hash class `${algorithm.qualifiedName}` must have a _single_ `${HashSize::class.qualifiedName}` annotation")
            }
        }
    }
}
