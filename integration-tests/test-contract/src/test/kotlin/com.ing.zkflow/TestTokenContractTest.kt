package com.ing.zkflow

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TestTokenContractTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `TestTokenState must make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            TestTokenContract.TestTokenState.serializer(),
            TestTokenContract.TestTokenState()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `~Create~ command must make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            TestTokenContract.Create.serializer(),
            TestTokenContract.Create(),
            shouldPrint = true
        ) { _, _ ->
            // because Create command contains no fields, the mere fact of it serialized and deserialized
            // means the round trip is successful.
        }
    }
}
