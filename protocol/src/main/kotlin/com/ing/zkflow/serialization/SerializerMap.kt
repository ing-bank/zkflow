package com.ing.zkflow.serialization

import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.utilities.loggerFor
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import kotlin.reflect.KClass

object SerializersModuleRegistry {
    private val log = loggerFor<com.ing.zkflow.serialization.SerializersModuleRegistry>()
    private val modules = mutableListOf(CordaSerializers.module)

    /**
     * Register a SerializersModule
     */
    fun register(module: SerializersModule) {
        com.ing.zkflow.serialization.SerializersModuleRegistry.log.debug("Registering SerializersModule: $module")
        com.ing.zkflow.serialization.SerializersModuleRegistry.modules.add(module)
    }

    /**
     * Returns a merged SerializersModule.
     *
     * This module contains the merged contents of all registered modules.
     * Please note that modules that were registered later overwrite serializers that are present in previously
     * registered modules. This allows users to customize the behaviour of core serializers if required.
     */
    val merged: SerializersModule by lazy {
        com.ing.zkflow.serialization.SerializersModuleRegistry.modules.reduce { previous, next ->
            previous.overwriteWith(next)
        }
    }
}

object ContractStateSerializerMap : com.ing.zkflow.serialization.SerializerMap<ContractState>()
object CommandDataSerializerMap : com.ing.zkflow.serialization.SerializerMap<CommandData>()

abstract class SerializerMap<T : Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val obj2Id = mutableMapOf<KClass<out T>, Int>()
    private val objId2Serializer = mutableMapOf<Int, KSerializer<out T>>()

    fun register(klass: KClass<out T>, id: Int, strategy: KSerializer<out T>) {
        log.debug("Registering serializer $strategy for $klass")
        obj2Id.put(klass, id)?.let { throw com.ing.zkflow.serialization.SerializerMapError.ClassAlreadyRegistered(klass, it) }
        objId2Serializer.put(id, strategy)?.let {
            throw com.ing.zkflow.serialization.SerializerMapError.IdAlreadyRegistered(
                id,
                klass,
                it.descriptor.serialName
            )
        }
    }

    fun prefixWithIdentifier(klass: KClass<*>, body: ByteArray): ByteArray {
        val id = obj2Id[klass] ?: throw com.ing.zkflow.serialization.SerializerMapError.ClassNotRegistered(klass)
        return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(id).array() + body
    }

    fun extractSerializerAndSerializedData(message: ByteArray): Pair<KSerializer<out T>, ByteArray> {
        return Pair(extractSerializer(message), extractSerializedData(message))
    }

    private fun extractSerializer(message: ByteArray): KSerializer<out T> {
        val stamp = extractIdentifier(message)
        return objId2Serializer[stamp] ?: throw com.ing.zkflow.serialization.SerializerMapError.ClassNotRegistered(stamp)
    }

    operator fun get(klass: KClass<*>): KSerializer<out T> =
        obj2Id[klass]?.let { objId2Serializer[it] } ?: error("$klass is not registered")

    private fun extractSerializedData(message: ByteArray): ByteArray = message.drop(Int.SIZE_BYTES).toByteArray()
    private fun extractIdentifier(message: ByteArray): Int = ByteBuffer.wrap(message.copyOfRange(0, Int.SIZE_BYTES)).int
}
