package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.PairSerializer

/**
 * Pair serializer.
 */
 /*
 The reason why its naming deviates from more conventional, say, IntSerializer is the inconsistent accessibility pattern:
 `public fun Int.Companion.serializer(): KSerializer<Int> = CharSerializer` (internal to kotlinx)
 vs
 `public fun <K, V> PairSerializer(...): ... = kotlinx.serialization.internal.PairSerializer(keySerializer, valueSerializer)`
 */
open class PairSerializerWithDefault<A, B>(
    firstSerializer: FixedLengthKSerializerWithDefault<A>,
    secondSerializer: FixedLengthKSerializerWithDefault<B>,
) : FixedLengthKSerializerWithDefault<Pair<A, B>>, KSerializer<Pair<A, B>> by PairSerializer(firstSerializer, secondSerializer) {
    override val default: Pair<A, B> = Pair(firstSerializer.default, secondSerializer.default)
    override val descriptor = FixedLengthSerialDescriptor(
        PairSerializer(firstSerializer, secondSerializer).descriptor,
        firstSerializer.descriptor.byteSize + secondSerializer.descriptor.byteSize
    )
}
