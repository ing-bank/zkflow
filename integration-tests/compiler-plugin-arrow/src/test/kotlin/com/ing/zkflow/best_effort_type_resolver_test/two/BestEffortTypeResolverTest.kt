package com.ing.zkflow.best_effort_type_resolver_test.two

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.BFLEngine
import io.kotest.matchers.shouldBe
import org.junit.Test

class BestEffortTypeResolverTest : SerializerTest {
    @Test
    fun `correct TestMe is found`() {
        val serialization = BFLEngine.Bytes.serialize(TesterTwo.serializer(), TesterTwo())

        // serialization must correspond to a serialization of an ASCII string "aa" with max of 5 characters.
        // see definition of `com.ing.zkflow.best_effort_type_resolver_test.two.TestMe`
        serialization shouldBe byteArrayOf(0, 0, 0, 2, 97, 97, 0, 0, 0)
    }

    @ZKP
    data class TesterTwo(val testMe: TestMe = TestMe())
}
