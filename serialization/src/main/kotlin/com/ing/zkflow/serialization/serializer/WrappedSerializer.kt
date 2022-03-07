@file:Suppress("MatchingDeclarationName")
package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializer
import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

/**
 * Convenience serializers to circumvent impossibility to extend from objects.
 * For example, the next line is impossible
 * object Long_2 : LongSerializer
 * while this one is possible:
 * object Long_2 : WrappedFixedLengthKSerializerWithDefault<Long>(LongSerializer)
 */

abstract class WrappedFixedLengthKSerializer<T : Any>(strategy: KSerializer<T>, val kClass: KClass<T>) : FixedLengthKSerializer<T>, KSerializer<T> by strategy {
    override val descriptor = if (kClass.java.isEnum) {
        FixedLengthSerialDescriptor(strategy.descriptor, Int.SIZE_BYTES)
    } else {
        strategy.descriptor.toFixedLengthSerialDescriptorOrThrow()
    }
}

abstract class WrappedFixedLengthKSerializerWithDefault<T>(strategy: FixedLengthKSerializerWithDefault<T>) : FixedLengthKSerializerWithDefault<T> by strategy
