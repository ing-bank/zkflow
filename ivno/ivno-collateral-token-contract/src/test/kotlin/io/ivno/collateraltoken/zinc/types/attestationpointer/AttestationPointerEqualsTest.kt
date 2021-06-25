package io.ivno.collateraltoken.zinc.types.attestationpointer

import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.zinc.types.attestationPointer
import io.ivno.collateraltoken.zinc.types.attestationPointerWithDifferentStateClass
import io.ivno.collateraltoken.zinc.types.attestationPointerWithDifferentStateLinearId
import io.ivno.collateraltoken.zinc.types.attestationPointerWithDifferentStateRef
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.identityframework.contract.AttestationPointer
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AttestationPointerEqualsTest {
    private val zincZKService = getZincZKService<AttestationPointerEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `attestationPointer equality test`(left: AttestationPointer<*>, right: AttestationPointer<*>, expected: Boolean) {
        performEqualityTest(left, right, expected)
    }

    private fun performEqualityTest(
        left: AttestationPointer<*>,
        right: AttestationPointer<*>,
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
        @JvmStatic
        fun testData() = listOf(
            Arguments.of(attestationPointer, attestationPointer, true),
            Arguments.of(attestationPointer, attestationPointerWithDifferentStateRef, false),
            Arguments.of(attestationPointer, attestationPointerWithDifferentStateClass, false),
            Arguments.of(attestationPointer, attestationPointerWithDifferentStateLinearId, false),
        )

    }
}