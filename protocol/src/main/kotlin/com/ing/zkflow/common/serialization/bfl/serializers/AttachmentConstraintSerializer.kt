package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
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
@SerialName("e")
object AlwaysAcceptAttachmentConstraintSurrogate : Surrogate<AlwaysAcceptAttachmentConstraint> {
    override fun toOriginal() = AlwaysAcceptAttachmentConstraint
}

@Serializable
@SerialName("f")
data class HashAttachmentConstraintSurrogate(
    val attachmentId: @Contextual SecureHash,
) : Surrogate<HashAttachmentConstraint> {
    override fun toOriginal(): HashAttachmentConstraint {
        return HashAttachmentConstraint(attachmentId)
    }
}

@Serializable
@SerialName("g")
object WhitelistedByZoneAttachmentConstraintSurrogate : Surrogate<WhitelistedByZoneAttachmentConstraint> {
    override fun toOriginal() = WhitelistedByZoneAttachmentConstraint
}

@Serializable
@SerialName("h")
object AutomaticPlaceholderConstraintSurrogate : Surrogate<AutomaticPlaceholderConstraint> {
    override fun toOriginal() = AutomaticPlaceholderConstraint
}

@Serializable
@SerialName("i")
data class SignatureAttachmentConstraintSurrogate(
    val publicKey: PublicKey,
) : Surrogate<SignatureAttachmentConstraint> {
    override fun toOriginal(): SignatureAttachmentConstraint {
        return SignatureAttachmentConstraint(publicKey)
    }
}

@Serializable
@SerialName("j")
object AutomaticHashConstraintSurrogate : Surrogate<AutomaticHashConstraint> {
    override fun toOriginal() = AutomaticHashConstraint
}

object AlwaysAcceptAttachmentConstraintSerializer :
    SurrogateSerializer<AlwaysAcceptAttachmentConstraint, AlwaysAcceptAttachmentConstraintSurrogate>(
        AlwaysAcceptAttachmentConstraintSurrogate.serializer(),
        { AlwaysAcceptAttachmentConstraintSurrogate }
    )

object HashAttachmentConstraintSerializer :
    SurrogateSerializer<HashAttachmentConstraint, HashAttachmentConstraintSurrogate>(
        HashAttachmentConstraintSurrogate.serializer(),
        { HashAttachmentConstraintSurrogate(attachmentId = it.attachmentId) }
    )

object WhitelistedByZoneAttachmentConstraintSerializer :
    SurrogateSerializer<WhitelistedByZoneAttachmentConstraint, WhitelistedByZoneAttachmentConstraintSurrogate>(
        WhitelistedByZoneAttachmentConstraintSurrogate.serializer(),
        { WhitelistedByZoneAttachmentConstraintSurrogate }
    )

object AutomaticPlaceholderConstraintSerializer :
    SurrogateSerializer<AutomaticPlaceholderConstraint, AutomaticPlaceholderConstraintSurrogate>(
        AutomaticPlaceholderConstraintSurrogate.serializer(),
        { AutomaticPlaceholderConstraintSurrogate }
    )

object SignatureAttachmentConstraintSerializer :
    SurrogateSerializer<SignatureAttachmentConstraint, SignatureAttachmentConstraintSurrogate>(
        SignatureAttachmentConstraintSurrogate.serializer(),
        { SignatureAttachmentConstraintSurrogate(publicKey = it.key) }
    )

object AutomaticHashConstraintSerializer :
    SurrogateSerializer<AutomaticHashConstraint, AutomaticHashConstraintSurrogate>(
        AutomaticHashConstraintSurrogate.serializer(),
        { AutomaticHashConstraintSurrogate }
    )
