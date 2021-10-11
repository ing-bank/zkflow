package com.ing.zkflow.testing.serialization

import com.ing.serialization.bfl.api.serialize
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import com.ing.zkflow.testing.bytesToWitness
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializationMagic
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.serialization.serialize
import net.corda.core.utilities.ByteSequence

public fun getSerializationContext(
    schemeId: Int,
    additionalSerializationProperties: Map<Any, Any> = emptyMap()
): SerializationContext {
    val magic: SerializationMagic = CustomSerializationSchemeUtils.getCustomSerializationMagicFromSchemeId(schemeId)
    return SerializationDefaults.P2P_CONTEXT.withPreferredSerializationVersion(magic)
        .withProperties(additionalSerializationProperties)
}

public fun <T : Any> T.serializeWithScheme(
    schemeId: Int,
    additionalSerializationProperties: Map<Any, Any> = emptyMap()
): ByteSequence {
    val serializationContext = getSerializationContext(schemeId, additionalSerializationProperties)
    return SerializationFactory.defaultFactory.withCurrentContext(serializationContext) { this.serialize() }
}

public fun <T : Any> toObliviousWitness(item: T, serializersModule: SerializersModule = EmptySerializersModule): String {
    val bytes = serialize(item, serializersModule = CordaSerializers.module + serializersModule)
    return bytesToWitness(bytes)
}
