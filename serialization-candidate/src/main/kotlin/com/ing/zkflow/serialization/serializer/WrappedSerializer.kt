package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer

/**
 * Convenience serializers to circumvent impossibility to extend from objects.
 * For example, the next line is impossible
 * object Long_2 : LongSerializer
 * while this one is possible:
 * object Long_2 : WrappedKSerializerWithDefault<Long>(LongSerializer)
 */
abstract class WrappedKSerializer<T>(strategy: KSerializer<T>) : KSerializer<T> by strategy
abstract class WrappedKSerializerWithDefault<T>(strategy: KSerializerWithDefault<T>) : KSerializerWithDefault<T> by strategy
