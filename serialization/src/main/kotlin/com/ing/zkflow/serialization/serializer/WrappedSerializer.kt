@file:Suppress("MatchingDeclarationName")
package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializer
import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.KSerializer

/**
 * Convenience serializers to circumvent impossibility to extend from objects.
 * For example, the next line is impossible
 * object Long_2 : LongSerializer
 * while this one is possible:
 * object Long_2 : WrappedFixedLengthKSerializerWithDefault<Long>(LongSerializer)
 */

abstract class WrappedFixedLengthKSerializer<T : Any>(strategy: KSerializer<T>, isEnum: Boolean) : FixedLengthKSerializer<T>, KSerializer<T> by strategy {
    override val descriptor = if (isEnum) {
        FixedLengthSerialDescriptor(strategy.descriptor, Int.SIZE_BYTES)
    } else {
        strategy.descriptor.toFixedLengthSerialDescriptorOrThrow()
    }
}

abstract class WrappedFixedLengthKSerializerWithDefault<T>(strategy: FixedLengthKSerializerWithDefault<T>) : FixedLengthKSerializerWithDefault<T> by strategy
