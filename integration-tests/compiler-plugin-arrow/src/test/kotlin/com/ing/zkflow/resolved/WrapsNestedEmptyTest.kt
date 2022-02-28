package com.ing.zkflow.resolved

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Serializable
data class WrapsNestedEmpty(
    val nested: WrapsEmpty = WrapsEmpty()
)

class WrapsNestedEmptyTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsNestedEmpty make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            com.ing.zkflow.annotated.WrapsNestedEmpty.serializer(),
            com.ing.zkflow.annotated.WrapsNestedEmpty()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `WrapsNestedEmpty generated and manual serializations must coincide`(engine: SerdeEngine) {
        engine.serialize(
            com.ing.zkflow.annotated.WrapsNestedEmpty.serializer(),
            com.ing.zkflow.annotated.WrapsNestedEmpty()
        ) shouldBe
            engine.serialize(WrapsNestedEmpty.serializer(), WrapsNestedEmpty())
    }
}
