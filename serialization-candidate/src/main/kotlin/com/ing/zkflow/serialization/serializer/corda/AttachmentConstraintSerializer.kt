@file:Suppress("DEPRECATION")

package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.serializer.ImmutableObjectSerializer
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint

object AlwaysAcceptAttachmentConstraintSerializer : KSerializerWithDefault<AlwaysAcceptAttachmentConstraint> by ImmutableObjectSerializer(AlwaysAcceptAttachmentConstraint)
object WhitelistedByZoneAttachmentConstraintSerializer : KSerializerWithDefault<WhitelistedByZoneAttachmentConstraint> by ImmutableObjectSerializer(WhitelistedByZoneAttachmentConstraint)
object AutomaticHashConstraintSerializer : KSerializerWithDefault<AutomaticHashConstraint> by ImmutableObjectSerializer(AutomaticHashConstraint)
object AutomaticPlaceholderConstraintSerializer : KSerializerWithDefault<AutomaticPlaceholderConstraint> by ImmutableObjectSerializer(AutomaticPlaceholderConstraint)

open class HashAttachmentConstraintSerializer(algorithm: String, hashLength: Int) : KSerializerWithDefault<HashAttachmentConstraint> {
    private val strategy = SecureHashSerializer(algorithm, hashLength)
    override val default = HashAttachmentConstraint(strategy.default)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HashAttachmentConstraint$algorithm") {
        element("attachmentId", strategy.descriptor)
    }

    override fun serialize(encoder: Encoder, value: HashAttachmentConstraint) =
        encoder.encodeSerializableValue(strategy, value.attachmentId)

    override fun deserialize(decoder: Decoder): HashAttachmentConstraint =
        HashAttachmentConstraint(decoder.decodeSerializableValue(strategy))
}

open class SignatureAttachmentConstraintSerializer(cordaSignatureId: Int) : KSerializerWithDefault<SignatureAttachmentConstraint> {
    private val strategy = PublicKeySerializer(cordaSignatureId)
    override val default = SignatureAttachmentConstraint(strategy.default)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SignatureAttachmentConstraint${strategy.algorithmNameIdentifier}") {
        element("key", strategy.descriptor)
    }

    override fun serialize(encoder: Encoder, value: SignatureAttachmentConstraint) =
        encoder.encodeSerializableValue(strategy, value.key)

    override fun deserialize(decoder: Decoder): SignatureAttachmentConstraint =
        SignatureAttachmentConstraint(decoder.decodeSerializableValue(strategy))
}
