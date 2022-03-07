@file:Suppress("DEPRECATION")

package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.ImmutableObjectSerializer
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint

object AlwaysAcceptAttachmentConstraintSerializer : FixedLengthKSerializerWithDefault<AlwaysAcceptAttachmentConstraint> by ImmutableObjectSerializer(AlwaysAcceptAttachmentConstraint)
object WhitelistedByZoneAttachmentConstraintSerializer : FixedLengthKSerializerWithDefault<WhitelistedByZoneAttachmentConstraint> by ImmutableObjectSerializer(WhitelistedByZoneAttachmentConstraint)
object AutomaticHashConstraintSerializer : FixedLengthKSerializerWithDefault<AutomaticHashConstraint> by ImmutableObjectSerializer(AutomaticHashConstraint)
object AutomaticPlaceholderConstraintSerializer : FixedLengthKSerializerWithDefault<AutomaticPlaceholderConstraint> by ImmutableObjectSerializer(AutomaticPlaceholderConstraint)

object HashAttachmentConstraintSerializer : FixedLengthKSerializerWithDefault<HashAttachmentConstraint> {
    private val strategy = SHA256SecureHashSerializer // SHA-256 is hardcoded in Corda for attachment ids
    override val default = HashAttachmentConstraint(strategy.default)
    override val descriptor = buildClassSerialDescriptor("HashAttachmentConstraint") {
        element("attachmentId", strategy.descriptor)
    }.toFixedLengthSerialDescriptorOrThrow()

    override fun serialize(encoder: Encoder, value: HashAttachmentConstraint) =
        encoder.encodeSerializableValue(strategy, value.attachmentId)

    override fun deserialize(decoder: Decoder): HashAttachmentConstraint =
        HashAttachmentConstraint(decoder.decodeSerializableValue(strategy))
}

open class SignatureAttachmentConstraintSerializer(cordaSignatureId: Int) :
    FixedLengthKSerializerWithDefault<SignatureAttachmentConstraint> {
    private val strategy = PublicKeySerializer(cordaSignatureId)
    override val default = SignatureAttachmentConstraint(strategy.default)
    override val descriptor = buildClassSerialDescriptor("SignatureAttachmentConstraint${strategy.algorithmNameIdentifier}") {
        element("key", strategy.descriptor)
    }.toFixedLengthSerialDescriptorOrThrow()

    override fun serialize(encoder: Encoder, value: SignatureAttachmentConstraint) =
        encoder.encodeSerializableValue(strategy, value.key)

    override fun deserialize(decoder: Decoder): SignatureAttachmentConstraint =
        SignatureAttachmentConstraint(decoder.decodeSerializableValue(strategy))
}
