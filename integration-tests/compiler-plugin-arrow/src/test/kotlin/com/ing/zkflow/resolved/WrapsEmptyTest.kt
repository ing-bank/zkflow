package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Serializable
class WrapsEmpty {
    override fun hashCode(): Int = 1

    override fun equals(other: Any?): Boolean = when (other) {
        is WrapsEmpty -> true
        else -> false
    }
}

class WrapsEmptyTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsEmpty make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsEmpty.serializer(),
            com.ing.zkflow.annotated.WrapsEmpty()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsEmpty generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsEmpty.serializer(),
            com.ing.zkflow.annotated.WrapsEmpty()
        ) shouldBe
            engine.serialize(WrapsEmpty.serializer(), WrapsEmpty())
    }
}
