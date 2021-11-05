package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer

interface KSerializerWithDefault<T> : KSerializer<T> {
    val default: T
}
