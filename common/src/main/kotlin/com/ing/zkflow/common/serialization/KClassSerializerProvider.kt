package com.ing.zkflow.common.serialization

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

/**
 * Interface to allow registration of serializers through providers.
 * At runtime, all such providers will be picked up and injected to
 * [BFLSerializationScheme]
 *
 * To make sure that the `serializer` matches the `klass`,
 * the type parameter for `klass` and `serializer` must be the same type.
 */
data class KClassSerializer<out T : Any>(
    val klass: KClass<out T>,
    /**
     * This id is a unique and stable identifier for the `klass` property and its `serializer`.
     * The relation between `id` and `klass` is used by the BFL serialization scheme as a short identifier to put in the serialized payload
     * to reduce the size of the serialized payload.
     *
     * On deserialization, the id is then used to look up the correct `klass` and `serializer`.
     */
    val id: Int,
    val serializer: KSerializer<out T>
)

interface KClassSerializerProvider {
    fun get(): KClassSerializer<*>
}
