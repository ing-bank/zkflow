package io.ivno.collateraltoken.zinc.types.deposit

import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.zinc.types.deposit
import io.ivno.collateraltoken.zinc.types.depositWithAnotherCustodian
import io.ivno.collateraltoken.zinc.types.depositWithAnotherDepositor
import io.ivno.collateraltoken.zinc.types.depositWithAnotherTokenIssuer
import io.ivno.collateraltoken.zinc.types.depositWithDifferentAccountId
import io.ivno.collateraltoken.zinc.types.depositWithDifferentAmountQuantity
import io.ivno.collateraltoken.zinc.types.depositWithDifferentAmountType
import io.ivno.collateraltoken.zinc.types.depositWithDifferentLinearId
import io.ivno.collateraltoken.zinc.types.depositWithDifferentReference
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class DepositEqualsTest {
    private val zincZKService = getZincZKService<DepositEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `different network should not be equal`(testData: Data) {
        performEqualityTest(testData.first, testData.second, testData.areEqual)
    }

    private fun performEqualityTest(
        left: Deposit,
        right: Deposit,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {

        data class Data(val first: Deposit, val second: Deposit, val areEqual: Boolean)

        @JvmStatic
        fun testData() = listOf(
            Data(deposit, deposit, true),
            Data(deposit, depositWithAnotherDepositor, false),
            Data(deposit, depositWithAnotherCustodian, false),
            Data(deposit, depositWithAnotherTokenIssuer, false),
            Data(deposit, depositWithDifferentAmountQuantity, false),
            Data(deposit, depositWithDifferentAmountType, false),
            Data(deposit, depositWithDifferentAccountId, false),
            Data(deposit, depositWithDifferentLinearId, false),
            Data(deposit, depositWithDifferentReference, false),
        )
    }
}
