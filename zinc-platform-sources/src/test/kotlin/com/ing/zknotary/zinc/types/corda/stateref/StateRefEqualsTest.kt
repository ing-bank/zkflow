package com.ing.zknotary.zinc.types.corda.stateref

import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class StateRefEqualsTest {
    private val zincZKService = getZincZKService<StateRefEqualsTest>()

    @Test
    fun `identity test`() {
        val identity = StateRef(SecureHash.hashAs(SecureHash.SHA2_256, "identity".toByteArray()), 0)
        performEqualityTest(identity, identity, true)
    }

    @Test
    fun `StateRefs with different hashes should not be equal`() {
        val index = 0
        val first = StateRef(SecureHash.hashAs(SecureHash.SHA2_256, "first".toByteArray()), index)
        val second = StateRef(SecureHash.hashAs(SecureHash.SHA2_256, "second".toByteArray()), index)
        performEqualityTest(first, second, false)
    }

    @Test
    fun `StateRefs with different indices should not be equal`() {
        val hash = SecureHash.hashAs(SecureHash.SHA2_256, "first".toByteArray())
        val first = StateRef(hash, 0)
        val second = StateRef(hash, 1)
        performEqualityTest(first, second, false)
    }

    private fun performEqualityTest(
        left: StateRef,
        right: StateRef,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
