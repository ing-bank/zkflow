package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.crypto.SecureHash
import java.security.PublicKey

@Serializable
abstract class AttachmentConstraintBaseSurrogate {
    abstract val id: Int
    abstract val attachmentId: @Contextual SecureHash?
    abstract val publicKey: PublicKey?
}

@Serializable
data class AlwaysAcceptAttachmentConstraintSurrogate(
    override val id: Int = 0,
    override val attachmentId: @Contextual SecureHash? = null,
    override val publicKey: PublicKey? = null
) : AttachmentConstraintBaseSurrogate(), Surrogate<AlwaysAcceptAttachmentConstraint> {
    override fun toOriginal() = AlwaysAcceptAttachmentConstraint
}

@Serializable
data class HashAttachmentConstraintSurrogate(
    override val id: Int = 1,
    override val attachmentId: @Contextual SecureHash?,
    override val publicKey: PublicKey? = null
) : AttachmentConstraintBaseSurrogate(), Surrogate<HashAttachmentConstraint> {
    override fun toOriginal(): HashAttachmentConstraint {
        checkNotNull(attachmentId) { "Serialization of AttachmentConstraint is malformed" }
        return HashAttachmentConstraint(attachmentId)
    }
}

@Serializable
data class WhitelistedByZoneAttachmentConstraintSurrogate(
    override val id: Int = 2,
    override val attachmentId: @Contextual SecureHash? = null,
    override val publicKey: PublicKey? = null
) : AttachmentConstraintBaseSurrogate(), Surrogate<WhitelistedByZoneAttachmentConstraint> {
    override fun toOriginal() = WhitelistedByZoneAttachmentConstraint
}

@Serializable
data class AutomaticPlaceholderConstraintSurrogate(
    override val id: Int = 3,
    override val attachmentId: @Contextual SecureHash? = null,
    override val publicKey: PublicKey? = null
) : AttachmentConstraintBaseSurrogate(), Surrogate<AutomaticPlaceholderConstraint> {
    override fun toOriginal() = AutomaticPlaceholderConstraint
}

@Serializable
data class SignatureAttachmentConstraintSurrogate(
    override val id: Int = 4,
    override val attachmentId: @Contextual SecureHash? = null,
    override val publicKey: PublicKey?,
) : AttachmentConstraintBaseSurrogate(), Surrogate<SignatureAttachmentConstraint> {
    override fun toOriginal(): SignatureAttachmentConstraint {
        checkNotNull(publicKey) { "Serialization of AttachmentConstraint is malformed" }
        return SignatureAttachmentConstraint(publicKey)
    }
}

@Serializable
data class AutomaticHashConstraintSurrogate(
    override val id: Int = 5,
    @Contextual
    override val attachmentId: @Contextual SecureHash? = null,
    override val publicKey: PublicKey? = null,
) : AttachmentConstraintBaseSurrogate(), Surrogate<AutomaticHashConstraint> {
    override fun toOriginal() = AutomaticHashConstraint
}

object AlwaysAcceptAttachmentConstraintSerializer : KSerializer<AlwaysAcceptAttachmentConstraint> by (
    SurrogateSerializer(AlwaysAcceptAttachmentConstraintSurrogate.serializer()) { AlwaysAcceptAttachmentConstraintSurrogate() }
    )

object HashAttachmentConstraintSerializer : KSerializer<HashAttachmentConstraint> by (
    SurrogateSerializer(HashAttachmentConstraintSurrogate.serializer()) {
        HashAttachmentConstraintSurrogate(attachmentId = it.attachmentId)
    }
    )

object WhitelistedByZoneAttachmentConstraintSerializer : KSerializer<WhitelistedByZoneAttachmentConstraint> by (
    SurrogateSerializer(WhitelistedByZoneAttachmentConstraintSurrogate.serializer()) { WhitelistedByZoneAttachmentConstraintSurrogate() }
    )

object AutomaticPlaceholderConstraintSerializer : KSerializer<AutomaticPlaceholderConstraint> by (
    SurrogateSerializer(AutomaticPlaceholderConstraintSurrogate.serializer()) { AutomaticPlaceholderConstraintSurrogate() }
    )

object SignatureAttachmentConstraintSerializer : KSerializer<SignatureAttachmentConstraint> by (
    SurrogateSerializer(SignatureAttachmentConstraintSurrogate.serializer()) {
        SignatureAttachmentConstraintSurrogate(publicKey = it.key)
    }
    )

object AutomaticHashConstraintSerializer : KSerializer<AutomaticHashConstraint> by (
    SurrogateSerializer(AutomaticHashConstraintSurrogate.serializer()) { AutomaticHashConstraintSurrogate() }
    )
