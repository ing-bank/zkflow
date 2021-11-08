package com.ing.zkflow.serialization.scheme

import com.ing.zkflow.engine.BFLEngine
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.SerializerTest
import com.ing.zkflow.serialization.utils.binary.Representation
import com.ing.zkflow.serialization.utils.binary.binary
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BFLDirectIntTest : SerializerTest {
    private val representation = Representation.BYTES
    private val engine = BFLEngine(representation)

    private val int = 1024
    private val direct = int.binary(representation)

    @Test
    fun `Int direct encoding and serialization must coincide`() {
        val ser = engine.serialize(IntSerializer, int)
        ser shouldBe direct
    }

    @Test
    fun `Int and deserialization must coincide`() {
        engine.deserialize(IntSerializer, direct) shouldBe int
    }
}
