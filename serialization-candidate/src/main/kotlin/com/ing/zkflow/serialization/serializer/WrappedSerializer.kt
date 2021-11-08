package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer

abstract class WrappedKSerializer<T>(strategy: KSerializer<T>) : KSerializer<T> by strategy
abstract class WrappedKSerializerWithDefault<T>(strategy: KSerializerWithDefault<T>) : KSerializerWithDefault<T> by strategy
