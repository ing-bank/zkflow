package com.ing.zknotary.common.contracts

import com.ing.zknotary.common.contracts.TestContract.TestState
import com.ing.zknotary.testing.fixed
import net.corda.core.crypto.Crypto
import net.corda.testing.core.TestIdentity
import org.junit.Test
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals

class FixedLengthGeneratedTest {
    private val alice = TestIdentity.fixed("alice", Crypto.EDDSA_ED25519_SHA512)

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `generated fixed state must have identical visible properties`() {
        val original = TestState(alice.party, 1)
        val fixed = FixedTestState(original)
        original::class.memberProperties.filter { it.visibility == KVisibility.PUBLIC }.forEach {
            it as KProperty1<Any, *>
            val origVal = it.get(original)

            val fixedProp = fixed::class.memberProperties.single { fixedProp -> fixedProp.name == it.name } as KProperty1<Any, *>
            val fixedVal = fixedProp.get(fixed)
            assertEquals(origVal, fixedVal)
        }
    }
}
