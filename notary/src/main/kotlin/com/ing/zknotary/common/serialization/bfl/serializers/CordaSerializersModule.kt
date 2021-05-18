package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSerializer
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

    // Contextual types.
    contextual(SecureHashSerializer)
    contextual(SecureHashSHA256Serializer)
    contextual(SecureHashHASHSerializer)

    contextual(AbstractPartySerializer)
    contextual(PartySerializer)
    contextual(AnonymousPartySerializer)

    contextual(CordaX500NameSerializer)

    contextual(UniqueIdentifierSerializer)
    contextual(StateRefSerializer)
    contextual(PrivacySaltSerializer)

    contextual(TimeWindowSerializer)
    contextual(LinearPointerSerializer)
    contextual(ZonedDateTimeSerializer)

    /**
     * This SerializersModule explicitly leaves out a KSerializer for PartyAndReference.
     * Users of this library should choose whether they need to retain the CordaX500Name of Party, as this has a
     * negative impact on the performance of the ZKP circuit.
     */
    // contextual(PartyAndReferenceSerializer)
    // contextual(AnonymisingPartyAndReferenceSerializer)
}
