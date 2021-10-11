package io.ivno.collateraltoken.zinc.types.claim

import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.ClaimSurrogate
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ClaimEqualsTest {
    private val zincZKService = getZincZKService<ClaimEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(claim, claim, true)
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `different claim should not be equal`(testPair: Pair<Claim<String>, Claim<String>>) {
        performEqualityTest(testPair.first, testPair.second, false)
    }

    private fun performEqualityTest(
        left: Claim<String>,
        right: Claim<String>,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(ClaimSurrogate.PROPERTY_LENGTH, VALUE_LENGTH)
            )
            put(
                "right",
                right.toJsonObject(ClaimSurrogate.PROPERTY_LENGTH, VALUE_LENGTH)
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        const val VALUE_LENGTH = 7

        val claim = Claim("Property 1", "Value 1")
        private val anotherClaimWithDifferentProperty = Claim("Property 2", "Value 1")
        private val anotherClaimWithDifferentValue = Claim("Property 1", "Value 2")

        @JvmStatic
        fun testData() = listOf(
            Pair(claim, anotherClaimWithDifferentProperty),
            Pair(claim, anotherClaimWithDifferentValue),
            Pair(anotherClaimWithDifferentProperty, anotherClaimWithDifferentValue)
        )

    }
}