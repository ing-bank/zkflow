package io.ivno.collateraltoken.zinc.types.attestation

import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.zinc.types.attestation
import io.ivno.collateraltoken.zinc.types.attestationWithDifferentAttestees
import io.ivno.collateraltoken.zinc.types.attestationWithDifferentAttestor
import io.ivno.collateraltoken.zinc.types.attestationWithDifferentLinearId
import io.ivno.collateraltoken.zinc.types.attestationWithDifferentPointer
import io.ivno.collateraltoken.zinc.types.attestationWithDifferentPreviousState
import io.ivno.collateraltoken.zinc.types.attestationWithDifferentStatus
import io.ivno.collateraltoken.zinc.types.attestationWithoutPreviousState
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.identityframework.contract.Attestation
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AttestationEqualsTest {
    private val zincZKService = getZincZKService<AttestationEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `attestation equality test`(left: Attestation<*>, right: Attestation<*>, expected: Boolean) {
        performEqualityTest(left, right, expected)
    }

    private fun performEqualityTest(
        left: Attestation<*>,
        right: Attestation<*>,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Arguments.of(attestation, attestation, true),
            Arguments.of(attestation, attestationWithDifferentAttestor, false),
            Arguments.of(attestation, attestationWithDifferentAttestees, false),
            Arguments.of(attestation, attestationWithDifferentPointer, false),
            Arguments.of(attestation, attestationWithDifferentStatus, false),
            Arguments.of(attestation, attestationWithDifferentLinearId, false),
            Arguments.of(attestation, attestationWithoutPreviousState, false),
            Arguments.of(attestation, attestationWithDifferentPreviousState, false),
        )
    }
}
