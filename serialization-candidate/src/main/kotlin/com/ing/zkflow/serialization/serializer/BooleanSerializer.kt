package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object BooleanSerializer : KSerializerWithDefault<Boolean>, KSerializer<Boolean> by Boolean.serializer() {
    override val default: Boolean = false
}
