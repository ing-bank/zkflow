package com.ing.zknotary.common.serialization.bfl.corda

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val CordaSerializers = SerializersModule {
    // Polymorphic types.
    //
    // Contextual types.
    contextual(SecureHashSerializer)
    contextual(SecureHashSHA256Serializer)
    contextual(SecureHashHASHSerializer)

    contextual(CordaX500NameSerializer)
    contextual(AbstractPartySerializer)

    contextual(UniqueIdentifierSerializer)
    contextual(StateRefSerializer)
}
