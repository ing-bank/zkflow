package io.ivno.collateraltoken.zinc.types.bigdecimalamount

import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class BigDecimalAmountLinearPointerEqualsTest {
    private val zincZKService = getZincZKService<BigDecimalAmountLinearPointerEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(bigDecimalAmount, bigDecimalAmount, true)
    }

    @Test
    fun `amounts with different quantity should not be equal`() {
        performEqualityTest(bigDecimalAmount, bigDecimalAmountWithDifferentQuantity, false)
    }

    @Test
    fun `amounts with different pointer should not be equal`() {
        performEqualityTest(bigDecimalAmount, bigDecimalAmountWithDifferentPointer, false)
    }

    private fun performEqualityTest(
        left: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        right: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        val bigDecimalAmount: BigDecimalAmount<LinearPointer<IvnoTokenType>> = BigDecimalAmount(
            BigDecimal.valueOf(42),
            LinearPointer(
                UniqueIdentifier(id = UUID(0, 42)),
                IvnoTokenType::class.java
            )
        )
        val bigDecimalAmountWithDifferentQuantity = bigDecimalAmount.copy(
            quantity = BigDecimal.valueOf(13)
        )
        val bigDecimalAmountWithDifferentPointer = bigDecimalAmount.copy(
            amountType = LinearPointer(
                UniqueIdentifier(id = UUID(0, 13)),
                IvnoTokenType::class.java
            )
        )
    }
}
