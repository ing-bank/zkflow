@file:Suppress("DEPRECATION")
package com.ing.zkflow.serialization.infra

import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint

@Serializable
data class TransactionStateSerializationMetadata(
    @Serializable(with = IntSerializer::class) val serializerId: Int,
    @Serializable(with = IntSerializer::class) val notarySignatureSchemeId: Int,
    val attachmentConstraintMetadata: AttachmentConstraintMetadata
)

@Suppress("ClassName")
@Serializable
data class AttachmentConstraintMetadata(
    @Serializable(with = IntSerializer::class) val serializerId: Int,
    @Serializable(with = HashAttachmentConstraintSpec_0::class) val hashAttachmentConstraintSpec: HashAttachmentConstraintSpec?,
    @Serializable(with = SignatureAttachmentConstraintSignatureSchemeId::class) val signatureAttachmentConstraintSignatureSchemeId: Int?,
) {
    object HashAttachmentConstraintSpec_0 : NullableSerializer<HashAttachmentConstraintSpec>(HashAttachmentConstraintSpec_1)
    object HashAttachmentConstraintSpec_1 : SerializerWithDefault<HashAttachmentConstraintSpec>(
        HashAttachmentConstraintSpec_2,
        HashAttachmentConstraintSpec("", 0)
    )
    object HashAttachmentConstraintSpec_2 : WrappedFixedLengthKSerializer<HashAttachmentConstraintSpec>(
        HashAttachmentConstraintSpec.serializer(),
        HashAttachmentConstraintSpec::class
    )

    object SignatureAttachmentConstraintSignatureSchemeId : NullableSerializer<Int>(IntSerializer)
}

@Serializable
data class HashAttachmentConstraintSpec(
    @Serializable(with = Algorithm::class) val algorithm: String,
    @Serializable(with = IntSerializer::class) val hashLength: Int
) {
    object Algorithm : FixedLengthASCIIStringSerializer(MAX_LENGTH_ALGORITHM_NAME)

    companion object {
        val MAX_LENGTH_ALGORITHM_NAME: Int = listOf(
            AlwaysAcceptAttachmentConstraint::class,
            HashAttachmentConstraint::class,
            WhitelistedByZoneAttachmentConstraint::class,
            AutomaticHashConstraint::class,
            AutomaticPlaceholderConstraint::class,
            SignatureAttachmentConstraint::class
        ).maxOf {
            "${it.qualifiedName}".length
        }
    }
}
