package com.ing.zknotary.common.serializer

import com.ing.zknotary.common.serializer.jackson.ZKJacksonSupport
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.ObjectWithCompatibleContext
import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.serialization.SerializeAsTokenContext
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.SingletonSerializationToken
import net.corda.core.utilities.ByteSequence

@CordaSerializable
object ZKJsonSerializationFactory : SerializationFactory(), SerializeAsToken {
    private val mapper = ZKJacksonSupport.createDefaultMapper(fullParties = true)

    override fun <T : Any> deserialize(byteSequence: ByteSequence, clazz: Class<T>, context: SerializationContext): T {
        return mapper.readValue(byteSequence.bytes, clazz)
    }

    override fun <T : Any> deserializeWithCompatibleContext(
        byteSequence: ByteSequence,
        clazz: Class<T>,
        context: SerializationContext
    ): ObjectWithCompatibleContext<T> {
        return ObjectWithCompatibleContext(mapper.readValue(byteSequence.bytes, clazz), context)
    }

    override fun <T : Any> serialize(obj: T, context: SerializationContext): SerializedBytes<T> {
        return SerializedBytes(mapper.writeValueAsBytes(obj))
    }

    private val token = SingletonSerializationToken.singletonSerializationToken(javaClass)
    override fun toToken(context: SerializeAsTokenContext) = token.registerWithContext(context, this)
}
