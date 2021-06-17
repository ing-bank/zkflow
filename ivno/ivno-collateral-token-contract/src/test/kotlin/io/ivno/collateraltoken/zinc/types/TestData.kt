package io.ivno.collateraltoken.zinc.types

import com.ing.zknotary.zinc.types.generateDifferentValueThan
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.contract.TransferInitiator
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.Claim
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
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

val stateRef = StateRef(SecureHash.zeroHash, 1)
val anotherStateRef = StateRef(SecureHash.allOnesHash, 1)

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

val transfer = Transfer(
    currentTokenHolder = anonymousParty,
    targetTokenHolder = anonymousParty,
    initiator = TransferInitiator.CURRENT_HOLDER,
    amount = amount,
    currentTokenHolderAccountId = "some-current",
    targetTokenHolderAccountId = "some-target",
    linearId = UniqueIdentifier(id = uuid)
)
val transferWithAnotherCurrentTokenHolder = transfer.copy(currentTokenHolder = anotherAnonymousParty)
val transferWithAnotherTargetTokenHolder = transfer.copy(targetTokenHolder = anotherAnonymousParty)
val transferWithAnotherInitiator = transfer.copy(initiator = TransferInitiator.TARGET_HOLDER)
val transferWithDifferentAmountQuantity = transfer.copy(amount = amountWithDifferentQuantity)
val transferWithDifferentAmountType = transfer.copy(amount = amountWithDifferentAmountType)
val transferWithDifferentCurrentAccountId = transfer.copy(currentTokenHolderAccountId = "another-current")
val transferWithDifferentTargetAccountId = transfer.copy(targetTokenHolderAccountId = "another-target")
val transferWithDifferentLinearId = transfer.copy(linearId = UniqueIdentifier(id = anotherUuid))

val abstractClaimWithString : AbstractClaim<String> = Claim("Property 1", "Value 1")
val claimWithString = abstractClaimWithString as Claim<String>
val anotherAbstractClaimWithString : AbstractClaim<String> = Claim("Property 2", "Value 2")
val anotherClaimWithString = anotherAbstractClaimWithString as Claim<String>
val abstractClaimWithInt : AbstractClaim<Int> = Claim("Property 1", 1)
val claimWithInt = abstractClaimWithInt as Claim<Int>
val anotherAbstractClaimWithInt : AbstractClaim<Int> = Claim("Property 2", 2)
val anotherClaimWithInt = anotherAbstractClaimWithInt as Claim<Int>
val abstractClaimWithContextual : AbstractClaim<StateRef> = Claim("Property 1", stateRef)
val claimWithContextual = abstractClaimWithContextual as Claim<StateRef>
val anotherAbstractClaimWithContextual : AbstractClaim<StateRef> = Claim("Property 2", anotherStateRef)
val anotherClaimWithContextual = anotherAbstractClaimWithContextual as Claim<StateRef>
val abstractClaimWithPolymorphic : AbstractClaim<AbstractParty> = Claim("Property 1", party)
val claimWithPolymorphic = abstractClaimWithPolymorphic as Claim<AbstractParty>
val anotherAbstractClaimWithPolymorphic : AbstractClaim<AbstractParty> = Claim("Property 2", anotherParty)
val anotherClaimWithPolymorphic = anotherAbstractClaimWithPolymorphic as Claim<AbstractParty>