package com.ing.zknotary.common.serialization

import net.corda.core.serialization.CustomSerializationScheme
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializationSchemeContext
import net.corda.core.serialization.serialize
import net.corda.core.utilities.ByteSequence

open class FixedLengthSerializationScheme : CustomSerializationScheme {

    companion object {
        const val SCHEME_ID = 777
    }

    override fun getSchemeId(): Int {
        return SCHEME_ID
    }

    override fun <T : Any> deserialize(
        bytes: ByteSequence,
        clazz: Class<T>,
        context: SerializationSchemeContext
    ): T {
        return SerializationFactory.defaultFactory.deserialize(bytes, clazz, SerializationDefaults.P2P_CONTEXT)
    }

    override fun <T : Any> serialize(obj: T, context: SerializationSchemeContext): ByteSequence {
        return obj.serialize(SerializationFactory.defaultFactory, SerializationDefaults.P2P_CONTEXT)
    }
}
