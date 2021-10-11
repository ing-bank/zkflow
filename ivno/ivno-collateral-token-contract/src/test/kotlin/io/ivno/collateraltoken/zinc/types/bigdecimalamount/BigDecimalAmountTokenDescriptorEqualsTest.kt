package io.ivno.collateraltoken.zinc.types.bigdecimalamount

import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.zinc.types.anotherTokenDescriptor
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.ivno.collateraltoken.zinc.types.tokenDescriptor
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BigDecimalAmountTokenDescriptorEqualsTest {
    private val zincZKService = getZincZKService<BigDecimalAmountTokenDescriptorEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(bigDecimalAmount, bigDecimalAmount, true)
    }

    @Test
    fun `amounts with different quantity should not be equal`() {
        performEqualityTest(bigDecimalAmount, bigDecimalAmountWithDifferentQuantity, false)
    }

    @Test
    fun `amounts with different token descriptor should not be equal`() {
        performEqualityTest(bigDecimalAmount, bigDecimalAmountWithDifferentTokenDescriptor, false)
    }

    private fun performEqualityTest(
        left: BigDecimalAmount<TokenDescriptor>,
        right: BigDecimalAmount<TokenDescriptor>,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        val bigDecimalAmount: BigDecimalAmount<TokenDescriptor> = BigDecimalAmount(
            BigDecimal.valueOf(42),
            tokenDescriptor
        )
        val bigDecimalAmountWithDifferentQuantity = bigDecimalAmount.copy(
            quantity = BigDecimal.valueOf(13)
        )
        val bigDecimalAmountWithDifferentTokenDescriptor = bigDecimalAmount.copy(
            amountType = anotherTokenDescriptor
        )
    }
}