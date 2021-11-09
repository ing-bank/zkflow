package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object IntSerializer : KSerializerWithDefault<Int>, KSerializer<Int> by Int.serializer() {
    override val default = 0
}
