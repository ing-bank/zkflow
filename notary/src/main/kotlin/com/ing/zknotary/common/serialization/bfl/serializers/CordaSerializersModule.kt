package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECPublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSAPublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256PublicKeySerializer
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSAPublicKeySerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.i2p.crypto.eddsa.EdDSAPublicKey
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
import org.bouncycastle.pqc.jcajce.provider.sphincs.BCSphincs256PublicKey
import java.security.PublicKey

@Suppress("UNCHECKED_CAST")
val CordaSerializers = SerializersModule {
    // Polymorphic types.
    // It is currently impossible to use polymorphicDefault for serialization,
    // see, https://github.com/Kotlin/kotlinx.serialization/issues/1317
    // polymorphicDefault(AttachmentConstraint::class) { AttachmentConstraintSerializer }
    polymorphic(AttachmentConstraint::class) {
        subclass(AlwaysAcceptAttachmentConstraint::class, AlwaysAcceptAttachmentConstraintSerializer)
        subclass(HashAttachmentConstraint::class, HashAttachmentConstraintSerializer)
        subclass(WhitelistedByZoneAttachmentConstraint::class, WhitelistedByZoneAttachmentConstraintSerializer)
        subclass(AutomaticPlaceholderConstraint::class, AutomaticPlaceholderConstraintSerializer)
        subclass(SignatureAttachmentConstraint::class, SignatureAttachmentConstraintSerializer)
        subclass(AutomaticHashConstraint::class, AutomaticHashConstraintSerializer)
    }

    polymorphic(PublicKey::class) {
        subclass(BCRSAPublicKey::class, BCRSAPublicKeySerializer)
        subclass(BCECPublicKey::class, BCECPublicKeySerializer)
        subclass(EdDSAPublicKey::class, EdDSAPublicKeySerializer)
        subclass(BCSphincs256PublicKey::class, BCSphincs256PublicKeySerializer)
    }

    // BFL treats abstract classes as polymorphic, since they have similar behaviour with interfaces
    polymorphic(AbstractParty::class) {
        subclass(AnonymousParty::class, AnonymousPartySerializer)
        subclass(Party::class, PartySerializer)
    }

    // Contextual types.
    contextual(SecureHashSerializer)
    contextual(SecureHashSHA256Serializer)
    contextual(SecureHashHASHSerializer)

    contextual(PartySerializer)
    contextual(AnonymousPartySerializer)

    contextual(CordaX500NameSerializer)

    contextual(UniqueIdentifierSerializer)
    contextual(StateRefSerializer)
    contextual(PrivacySaltSerializer)

    contextual(TimeWindowSerializer)
    contextual(PartyAndReferenceSerializer)
    contextual(LinearPointerSerializer)
    contextual(ZonedDateTimeSerializer)
}
