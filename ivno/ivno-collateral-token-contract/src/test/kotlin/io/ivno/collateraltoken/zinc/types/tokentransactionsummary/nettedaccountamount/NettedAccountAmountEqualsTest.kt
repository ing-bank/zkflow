package io.ivno.collateraltoken.zinc.types.tokentransactionsummary.nettedaccountamount

import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenTransactionSummary.NettedAccountAmount
import io.ivno.collateraltoken.zinc.types.nettedAccountAmount
import io.ivno.collateraltoken.zinc.types.nettedAccountAmountWithAccountAddressOfDifferentAccountId
import io.ivno.collateraltoken.zinc.types.nettedAccountAmountWithAccountAddressOfDifferentParty
import io.ivno.collateraltoken.zinc.types.nettedAccountAmountWithAmountOfDifferentQuantity
import io.ivno.collateraltoken.zinc.types.nettedAccountAmountWithAmountOfDifferentAmountType
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class NettedAccountAmountEqualsTest {
    private val zincZKService = getZincZKService<NettedAccountAmountEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `nettedAccountAmount equality test`(left: NettedAccountAmount, right: NettedAccountAmount, expected: Boolean) {
        performEqualityTest(left, right, expected)
    }

    private fun performEqualityTest(
        left: NettedAccountAmount,
        right: NettedAccountAmount,
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
            Arguments.of(nettedAccountAmount, nettedAccountAmount, true),
            Arguments.of(nettedAccountAmount, nettedAccountAmountWithAccountAddressOfDifferentAccountId, false),
            Arguments.of(nettedAccountAmount, nettedAccountAmountWithAccountAddressOfDifferentParty, false),
            Arguments.of(nettedAccountAmount, nettedAccountAmountWithAmountOfDifferentQuantity, false),
            Arguments.of(nettedAccountAmount, nettedAccountAmountWithAmountOfDifferentAmountType, false),
        )

    }
}