package io.ivno.collateraltoken.zinc.types

import com.ing.zknotary.zinc.types.generateDifferentValueThan
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import java.math.BigDecimal
import java.util.UUID
import kotlin.random.Random

fun randomUUID() = UUID(0, Random.nextInt(0, Int.MAX_VALUE).toLong())

val party: Party = TestIdentity.fresh("some-org").party
val anotherParty: Party = TestIdentity.fresh("another-org").party

val anonymousParty: AnonymousParty = TestIdentity.fresh("some-org").party.anonymise()
val anotherAnonymousParty: AnonymousParty = TestIdentity.fresh("another-org").party.anonymise()

val uuid = randomUUID()
val anotherUuid = generateDifferentValueThan(uuid) {
    randomUUID()
}

val amount: BigDecimalAmount<LinearPointer<IvnoTokenType>> = BigDecimalAmount(
    42, LinearPointer(UniqueIdentifier(id = uuid), IvnoTokenType::class.java)
)
val amountWithDifferentQuantity = amount.copy(quantity = BigDecimal.valueOf(13))
val amountWithDifferentAmountType = amount.copy(amountType = LinearPointer(UniqueIdentifier(id = anotherUuid), IvnoTokenType::class.java))

val deposit = Deposit(
    depositor = anonymousParty,
    custodian = anonymousParty,
    tokenIssuingEntity = anonymousParty,
    amount = amount,
    accountId = "some-account-id",
    linearId = UniqueIdentifier(id = uuid)
)
val depositWithAnotherDepositor = deposit.copy(depositor = anotherAnonymousParty)
val depositWithAnotherCustodian = deposit.copy(custodian = anotherAnonymousParty)
val depositWithAnotherTokenIssuer = deposit.copy(tokenIssuingEntity = anotherAnonymousParty)
val depositWithDifferentAmountQuantity = deposit.copy(amount = amountWithDifferentQuantity)
val depositWithDifferentAmountType = deposit.copy(amount = amountWithDifferentAmountType)
val depositWithDifferentAccountId = deposit.copy(accountId = "another-account-id")
val depositWithDifferentLinearId = deposit.copy(linearId = UniqueIdentifier(id = anotherUuid))
val depositWithDifferentReference = deposit.copy(reference = "some-reference")

val redemption = Redemption(
    redeemer = party,
    custodian = party,
    tokenIssuingEntity = party,
    amount = amount,
    accountId = "some-account-id",
    linearId = UniqueIdentifier(id = uuid)
)
val redemptionWithAnotherRedeemer = redemption.copy(redeemer = anotherParty)
val redemptionWithAnotherCustodian = redemption.copy(custodian = anotherParty)
val redemptionWithAnotherTokenIssuer = redemption.copy(tokenIssuingEntity = anotherParty)
val redemptionWithDifferentAmountQuantity = redemption.copy(amount = amountWithDifferentQuantity)
val redemptionWithDifferentAmountType = redemption.copy(amount = amountWithDifferentAmountType)
val redemptionWithDifferentAccountId = redemption.copy(accountId = "another-account-id")
val redemptionWithDifferentLinearId = redemption.copy(linearId = UniqueIdentifier(id = anotherUuid))
