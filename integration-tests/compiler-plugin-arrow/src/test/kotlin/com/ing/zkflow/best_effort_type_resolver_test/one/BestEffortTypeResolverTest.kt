package com.ing.zkflow.best_effort_type_resolver_test.one

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.BFLEngine
import io.kotest.matchers.shouldBe
import org.junit.Test

class BestEffortTypeResolverTest : SerializerTest {
    @Test
    fun `correct TestMe is found`() {
        val serialization = BFLEngine.Bytes.serialize(TesterOne.serializer(), TesterOne())
        serialization shouldBe byteArrayOf(0, 0, 0, 1)
    }

    @ZKP
    data class TesterOne(val testMe: TestMe = TestMe())
}
