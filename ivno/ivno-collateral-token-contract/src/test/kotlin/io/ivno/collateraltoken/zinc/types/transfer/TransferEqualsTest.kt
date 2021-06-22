package io.ivno.collateraltoken.zinc.types.transfer

import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.ivno.collateraltoken.zinc.types.transfer
import io.ivno.collateraltoken.zinc.types.transferWithAnotherCurrentTokenHolder
import io.ivno.collateraltoken.zinc.types.transferWithAnotherInitiator
import io.ivno.collateraltoken.zinc.types.transferWithAnotherTargetTokenHolder
import io.ivno.collateraltoken.zinc.types.transferWithDifferentAmountQuantity
import io.ivno.collateraltoken.zinc.types.transferWithDifferentAmountType
import io.ivno.collateraltoken.zinc.types.transferWithDifferentCurrentAccountId
import io.ivno.collateraltoken.zinc.types.transferWithDifferentLinearId
import io.ivno.collateraltoken.zinc.types.transferWithDifferentTargetAccountId
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TransferEqualsTest {
    private val zincZKService = getZincZKService<TransferEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `different network should not be equal`(testData: Data) {
        performEqualityTest(testData.first, testData.second, testData.areEqual)
    }

    private fun performEqualityTest(
        left: Transfer,
        right: Transfer,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {

        data class Data(val first: Transfer, val second: Transfer, val areEqual: Boolean)

        @JvmStatic
        fun testData() = listOf(
            Data(transfer, transfer, true),
            Data(transfer, transferWithAnotherCurrentTokenHolder, false),
            Data(transfer, transferWithAnotherTargetTokenHolder, false),
            Data(transfer, transferWithAnotherInitiator, false),
            Data(transfer, transferWithDifferentAmountQuantity, false),
            Data(transfer, transferWithDifferentAmountType, false),
            Data(transfer, transferWithDifferentCurrentAccountId, false),
            Data(transfer, transferWithDifferentTargetAccountId, false),
            Data(transfer, transferWithDifferentLinearId, false),
        )
    }
}
