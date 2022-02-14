package com.ing.zkflow.serialization

import kotlinx.serialization.KSerializer

/**
 * This interface must be implemented for all natively supported types
 * to remove requirement to annotate nullable type or inner types of collections with @Default annotation.
 */
interface FixedLengthKSerializerWithDefault<T> : FixedLengthKSerializer<T>, WithDefault<T>

interface FixedLengthKSerializer<T> : KSerializer<T> {
    override val descriptor: FixedLengthSerialDescriptor
}

interface WithDefault<T> {
    val default: T
}
