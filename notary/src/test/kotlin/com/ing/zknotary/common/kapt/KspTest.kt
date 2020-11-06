package com.ing.zknotary.common.kapt

import com.ing.zknotary.common.contracts.FixedTestState
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.zkp.ZKNulls
import org.junit.Test
import kotlin.reflect.full.memberProperties

class KspTest {
    @Test
    fun `Show generated FixedTestState`() {
        val original = TestContract.TestState(ZKNulls.NULL_PARTY, 1)
        val fixed = FixedTestState(original)
        println(fixed::class.memberProperties)
    }
}
