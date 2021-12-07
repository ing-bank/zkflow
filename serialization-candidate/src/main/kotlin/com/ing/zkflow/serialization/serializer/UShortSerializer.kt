package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object UShortSerializer : KSerializerWithDefault<UShort>, KSerializer<UShort> by UShort.serializer() {
    override val default: UShort = 0u
}
