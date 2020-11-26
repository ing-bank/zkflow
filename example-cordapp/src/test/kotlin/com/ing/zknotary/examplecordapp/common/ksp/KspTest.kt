package com.ing.zknotary.examplecordapp.common.ksp

import com.ing.zknotary.common.zkp.ZKNulls
import com.ing.zknotary.examplecordapp.contracts.FixedMyState
import com.ing.zknotary.examplecordapp.contracts.MyContract
import org.junit.Test
import kotlin.reflect.full.memberProperties

class KspTest {
    @Test
    fun `Show generated FixedMyState`() {
        val original = MyContract.MyState(ZKNulls.NULL_PARTY, 1)
        val fixed = FixedMyState(original)
        println(fixed::class.memberProperties)
    }
}
