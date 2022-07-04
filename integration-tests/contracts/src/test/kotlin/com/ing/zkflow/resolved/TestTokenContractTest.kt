package com.ing.zkflow.resolved

import com.ing.zkflow.annotated.TestTokenContract
import com.ing.zkflow.annotated.TestTokenContract_Create_Serializer
import com.ing.zkflow.annotated.TestTokenContract_TestTokenState_Serializer
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TestTokenContractTest : SerializerTest {
    @ParameterizedTest
    @MethodSource("engines")
    fun `TestTokenState must make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            TestTokenContract_TestTokenState_Serializer,
            TestTokenContract.TestTokenState()
        )
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `~Create~ command must make a round trip`(engine: SerdeEngine) {
        engine.assertRoundTrip(
            TestTokenContract_Create_Serializer,
            TestTokenContract.Create(),
            shouldPrint = true
        ) { _, _ ->
            // because Create command contains no fields, the mere fact of it being serialized and deserialized
            // means the round trip is successful.
        }
    }
}
