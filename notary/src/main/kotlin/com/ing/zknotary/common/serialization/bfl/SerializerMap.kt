package com.ing.zknotary.common.serialization.bfl

import kotlinx.serialization.KSerializer
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import java.nio.ByteBuffer
import kotlin.reflect.KClass

object ContractStateSerializerMap : SerializerMap<ContractState>()
object CommandDataSerializerMap : SerializerMap<CommandData>()

abstract class SerializerMap<T : Any> {
    private val obj2Id = mutableMapOf<KClass<out T>, Int>()
    private val objId2Serializer = mutableMapOf<Int, KSerializer<out T>>()

    fun register(klass: KClass<out T>, id: Int, strategy: KSerializer<out T>) {
        obj2Id.put(klass, id)?.let { throw SerializerMapError.ClassAlreadyRegistered(klass, id) }
        objId2Serializer.put(id, strategy)?.let { throw SerializerMapError.IdAlreadyRegistered(id, klass) }
    }

    fun prefixWithIdentifier(klass: KClass<*>, body: ByteArray): ByteArray {
        val id = obj2Id[klass] ?: throw SerializerMapError.ClassNotRegistered(klass)
        return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(id).array() + body
    }

    fun extractSerializerAndSerializedData(message: ByteArray): Pair<KSerializer<out T>, ByteArray> {
        return Pair(extractSerializer(message), extractSerializedData(message))
    }

    private fun extractSerializer(message: ByteArray): KSerializer<out T> {
        val stamp = extractIdentifier(message)
        return objId2Serializer[stamp] ?: throw SerializerMapError.ClassNotRegistered(stamp)
    }

    operator fun get(klass: KClass<*>): KSerializer<out T> =
        obj2Id[klass]?.let { objId2Serializer[it] } ?: error("State $klass is not registered")

    private fun extractSerializedData(message: ByteArray): ByteArray = message.drop(Int.SIZE_BYTES).toByteArray()
    private fun extractIdentifier(message: ByteArray): Int = ByteBuffer.wrap(message.copyOfRange(0, Int.SIZE_BYTES)).int
}
