package com.ing.zknotary.testcordapp.common.ksp

import com.ing.zknotary.common.zkp.ZKNulls
import com.ing.zknotary.testcordapp.contracts.FixedTestState
import com.ing.zknotary.testcordapp.contracts.MyContract
import org.junit.Test
import kotlin.reflect.full.memberProperties

class KspTest {
    @Test
    fun `Show generated FixedTestState`() {
        val original = MyContract.MyState(ZKNulls.NULL_PARTY, 1)
        val fixed = FixedTestState(original)
        println(fixed::class.memberProperties)
    }
}
