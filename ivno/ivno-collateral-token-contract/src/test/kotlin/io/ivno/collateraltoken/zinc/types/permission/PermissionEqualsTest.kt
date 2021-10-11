package io.ivno.collateraltoken.zinc.types.permission

import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.bnms.contract.Permission
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test

class PermissionEqualsTest {
    private val zincZKService = getZincZKService<PermissionEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(permission, permission, true)
    }

    @Test
    fun `different permissions should not be equal`() {
        performEqualityTest(permission, anotherPermission, false)
    }

    private fun performEqualityTest(
        left: Permission,
        right: Permission,
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
        val permission = Permission("Permission")
        val anotherPermission = Permission("Another Permission")
    }
}