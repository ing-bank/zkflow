package io.ivno.collateraltoken.zinc.types.redemption

import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSerializer
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.serialization.BigDecimalAmountSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import java.util.UUID

class DeserializeRedemptionTest :
DeserializationTestBase<DeserializeRedemptionTest, DeserializeRedemptionTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeRedemptionTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = SerializersModule {
        contextual(BigDecimalAmountSerializer(LinearPointerSerializer))
    }

    @Serializable
    data class Data(val data: Redemption)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            val party: Party = TestIdentity.fresh("test-org").party
            val amount: BigDecimalAmount<LinearPointer<IvnoTokenType>> = BigDecimalAmount(42, LinearPointer(
                UniqueIdentifier(id = UUID(0L, 32L)),
                IvnoTokenType::class.java
            ))
            return listOf(
                Data(
                    Redemption(
                        redeemer = party,
                        custodian = party,
                        tokenIssuingEntity = party,
                        amount = amount,
                        accountId = "account-id",
                        linearId = UniqueIdentifier(id = UUID(0L, 42L))
                    )
                ),
            )
        }
    }
}
