package com.ing.zkflow.serialization

import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.serializer
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.utilities.loggerFor
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import kotlin.reflect.KClass

object SerializersModuleRegistry {
    private val log = loggerFor<SerializersModuleRegistry>()
    private val modules = mutableListOf(CordaSerializers.module)

    /**
     * Register a SerializersModule
     */
    fun register(module: SerializersModule) {
        log.debug("Registering SerializersModule: $module")
        modules.add(module)
    }

    /**
     * Returns a merged SerializersModule.
     *
     * This module contains the merged contents of all registered modules.
     * Please note that modules that were registered later overwrite serializers that are present in previously
     * registered modules. This allows users to customize the behaviour of core serializers if required.
     */
    val merged: SerializersModule by lazy {
        modules.reduce { previous, next ->
            previous.overwriteWith(next)
        }
    }
}

object ContractStateSerializerMap : SerializerMap<ContractState>()
object CommandDataSerializerMap : SerializerMap<CommandData>()

abstract class SerializerMap<T : Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val obj2Id = mutableMapOf<KClass<out T>, Int>()
    private val objId2Serializer = mutableMapOf<Int, KSerializer<out T>>()

    fun register(klass: KClass<out T>, strategy: KSerializer<out T>? = null) {
        @Suppress("UNCHECKED_CAST") // If the serializer is found, we know it is for KClass<out T>, so we know it is KSerializer<out T>
        val serializer = strategy ?: SerializersModuleRegistry.merged.serializer(klass.java) as KSerializer<out T>

        val id = klass.hashCode() // Should be deterministic enough

        if (obj2Id[klass] == id && objId2Serializer[id] == serializer) {
            return // Idempotent if same args
        } else {
            log.debug("Registering serializer $serializer for $klass")
            obj2Id.put(klass, id)?.let { throw SerializerMapError.ClassAlreadyRegistered(klass, it) }
            objId2Serializer.put(id, serializer)?.let {
                throw SerializerMapError.IdAlreadyRegistered(
                    id,
                    klass,
                    it.descriptor.serialName
                )
            }
        }
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
        obj2Id[klass]?.let { objId2Serializer[it] } ?: throw SerializerMapError.ClassNotRegistered(klass)

    private fun extractSerializedData(message: ByteArray): ByteArray = message.drop(Int.SIZE_BYTES).toByteArray()
    private fun extractIdentifier(message: ByteArray): Int = ByteBuffer.wrap(message.copyOfRange(0, Int.SIZE_BYTES)).int
}
