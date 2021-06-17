package io.ivno.collateraltoken.zinc.types.redemption

import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.generateDifferentValueThan
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.UUID
import kotlin.random.Random

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
        private fun randomUUID() = UUID(0, Random.nextInt(0, Int.MAX_VALUE).toLong())

        private val party: Party = TestIdentity.fresh("some-org").party
        private val anotherParty: Party = TestIdentity.fresh("another-org").party

        private val uuid = randomUUID()
        private val anotherUuid = generateDifferentValueThan(uuid) {
            randomUUID()
        }

        private val amount: BigDecimalAmount<LinearPointer<IvnoTokenType>> = BigDecimalAmount(
            42, LinearPointer(UniqueIdentifier(id = uuid), IvnoTokenType::class.java)
        )
        private val amountWithDifferentQuantity = amount.copy(quantity = BigDecimal.valueOf(13))
        private val amountWithDifferentAmountType = amount.copy(amountType = LinearPointer(UniqueIdentifier(id = anotherUuid), IvnoTokenType::class.java))

        private val redemption = Redemption(
            redeemer = party,
            custodian = party,
            tokenIssuingEntity = party,
            amount = amount,
            accountId = "some-account-id",
            linearId = UniqueIdentifier(id = uuid)
        )
        private val redemptionWithAnotherRedeemer = redemption.copy(redeemer = anotherParty)
        private val redemptionWithAnotherCustodian = redemption.copy(custodian = anotherParty)
        private val redemptionWithAnotherTokenIssuer = redemption.copy(tokenIssuingEntity = anotherParty)
        private val redemptionWithDifferentAmountQuantity = redemption.copy(amount = amountWithDifferentQuantity)
        private val redemptionWithDifferentAmountType = redemption.copy(amount = amountWithDifferentAmountType)
        private val redemptionWithDifferentAccountId = redemption.copy(accountId = "another-account-id")
        private val redemptionWithDifferentLinearId = redemption.copy(linearId = UniqueIdentifier(id = anotherUuid))

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
