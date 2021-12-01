package com.ing.zkflow.serialization

import com.ing.zkflow.serialization.engine.BFLEngine

public interface SerializerTest {
    public companion object {
        @JvmStatic
        public fun engines(): List<BFLEngine> = listOf(BFLEngine.Bits, BFLEngine.Bytes)
    }
}
