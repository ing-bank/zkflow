package io.ivno.collateraltoken.zinc.types.redemption

import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.zinc.types.redemption
import io.ivno.collateraltoken.zinc.types.redemptionWithAnotherCustodian
import io.ivno.collateraltoken.zinc.types.redemptionWithAnotherRedeemer
import io.ivno.collateraltoken.zinc.types.redemptionWithAnotherTokenIssuer
import io.ivno.collateraltoken.zinc.types.redemptionWithDifferentAccountId
import io.ivno.collateraltoken.zinc.types.redemptionWithDifferentAmountQuantity
import io.ivno.collateraltoken.zinc.types.redemptionWithDifferentAmountType
import io.ivno.collateraltoken.zinc.types.redemptionWithDifferentLinearId
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RedemptionEqualsTest {
    private val zincZKService = getZincZKService<RedemptionEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `different network should not be equal`(testData: Data) {
        performEqualityTest(testData.first, testData.second, testData.areEqual)
    }

    private fun performEqualityTest(
        left: Redemption,
        right: Redemption,
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
        data class Data(val first: Redemption, val second: Redemption, val areEqual: Boolean)

        @JvmStatic
        fun testData() = listOf(
            Data(redemption, redemption, true),
            Data(redemption, redemptionWithAnotherRedeemer, false),
            Data(redemption, redemptionWithAnotherCustodian, false),
            Data(redemption, redemptionWithAnotherTokenIssuer, false),
            Data(redemption, redemptionWithDifferentAmountQuantity, false),
            Data(redemption, redemptionWithDifferentAmountType, false),
            Data(redemption, redemptionWithDifferentAccountId, false),
            Data(redemption, redemptionWithDifferentLinearId, false),
        )
    }
}
