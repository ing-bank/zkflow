package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.engine.BFLEngine

interface SerializerTest {
    companion object {
        @JvmStatic
        fun engines() = listOf(BFLEngine.Bits, BFLEngine.Bytes)
    }
}
