package com.ing.zknotary.common.serialization.bfl

import net.corda.core.serialization.CustomSerializationScheme
import net.corda.core.serialization.SerializationSchemeContext
import net.corda.core.utilities.ByteSequence

open class FixedLengthSerializationScheme : CustomSerializationScheme {
    companion object {
        const val SCHEME_ID = 120
    }

    override fun getSchemeId(): Int {
        return SCHEME_ID
    }

    override fun <T : Any> deserialize(
        bytes: ByteSequence,
        clazz: Class<T>,
        context: SerializationSchemeContext
    ): T {
        TODO("Deserializer says Sowwy")
        // return SerializationFactory.defaultFactory.deserialize(bytes, clazz, SerializationDefaults.P2P_CONTEXT)
    }

    override fun <T : Any> serialize(obj: T, context: SerializationSchemeContext): ByteSequence {
        TODO("Serializer says Sowwy")
    }
}
