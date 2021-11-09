package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer

/**
 * This interface must be implemented for all natively supported types
 * to remove requirement to annotate nullable type or inner types of collections with @Default annotation.
 */
interface KSerializerWithDefault<T> : KSerializer<T> {
    val default: T
}
