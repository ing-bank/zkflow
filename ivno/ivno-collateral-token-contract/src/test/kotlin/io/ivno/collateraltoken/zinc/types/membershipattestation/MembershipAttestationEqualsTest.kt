package io.ivno.collateraltoken.zinc.types.membershipattestation

import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.zinc.types.anotherMembershipAttestation
import io.ivno.collateraltoken.zinc.types.membershipAttestation
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class MembershipAttestationEqualsTest {
    private val zincZKService = getZincZKService<MembershipAttestationEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `membershipAttestation equality test`(left: MembershipAttestation, right: MembershipAttestation, expected: Boolean) {
        performEqualityTest(left, right, expected)
    }

    private fun performEqualityTest(
        left: MembershipAttestation,
        right: MembershipAttestation,
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
            Arguments.of(membershipAttestation, membershipAttestation, true),
            Arguments.of(membershipAttestation, anotherMembershipAttestation, false),
        )
    }
}
