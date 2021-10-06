package io.ivno.collateraltoken.zinc.types.role

import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.bnms.contract.Role
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test

class RoleEqualsTest {
    private val zincZKService = getZincZKService<RoleEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(role, role, true)
    }

    @Test
    fun `different roles should not be equal`() {
        performEqualityTest(role, anotherRole, false)
    }

    private fun performEqualityTest(
        left: Role,
        right: Role,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject()
            )
            put(
                "right",
                right.toJsonObject()
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        val role: Role = Role("Thief")
        val anotherRole: Role = Role("Warrior")
    }
}