package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer

open class WrappedKSerializer<T>(strategy: KSerializer<T>) : KSerializer<T> by strategy
open class WrappedKSerializerWithDefault<T>(strategy: KSerializerWithDefault<T>) : KSerializerWithDefault<T> by strategy
