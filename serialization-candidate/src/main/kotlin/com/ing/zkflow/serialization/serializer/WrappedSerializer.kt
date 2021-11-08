package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer

/**
 * Convenience serializers to ensure uniform treatment of all type independently whether they are native or not.
 */
abstract class WrappedKSerializer<T>(strategy: KSerializer<T>) : KSerializer<T> by strategy
abstract class WrappedKSerializerWithDefault<T>(strategy: KSerializerWithDefault<T>) : KSerializerWithDefault<T> by strategy
