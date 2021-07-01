package io.ivno.collateraltoken.zinc.types

import com.ing.zknotary.zinc.types.generateDifferentValueThan
import io.dasl.contracts.v1.account.AccountAddress
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenContract
import io.dasl.contracts.v1.token.TokenDescriptor
import io.dasl.contracts.v1.token.TokenTransactionSummary.NettedAccountAmount
import io.dasl.contracts.v1.token.TokenTransactionSummary.State
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.contract.TransferInitiator
import io.ivno.collateraltoken.serialization.MembershipAttestationSurrogate
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Setting
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.Attestation
import io.onixlabs.corda.identityframework.contract.AttestationPointer
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

fun randomUUID() = UUID(0, Random.nextInt(0, Int.MAX_VALUE).toLong())

val party: Party = TestIdentity.fresh("some-org").party
val anotherParty: Party = TestIdentity.fresh("another-org").party
val anEvenOtherParty: Party = TestIdentity.fresh("even-other-org").party

val anonymousParty: AnonymousParty = TestIdentity.fresh("some-org").party.anonymise()
val anotherAnonymousParty: AnonymousParty = TestIdentity.fresh("another-org").party.anonymise()
val anEvenOtherAnonymousParty: AnonymousParty = TestIdentity.fresh("even-other-org").party.anonymise()

val uuid = randomUUID()
val anotherUuid = generateDifferentValueThan(uuid) {
    randomUUID()
}

val instant: Instant = Instant.now()
val anotherInstant: Instant = instant.plusSeconds(42)

val stateRef = StateRef(SecureHash.zeroHash, 1)
val anotherStateRef = StateRef(SecureHash.allOnesHash, 1)

val someUniqueIdentifier = UniqueIdentifier(externalId = "some.id", id = UUID(0, 1))
val anotherUniqueIdentifier = UniqueIdentifier(externalId = "some.other.id", id = UUID(0, 2))

val amount: BigDecimalAmount<LinearPointer<IvnoTokenType>> = BigDecimalAmount(
    42, LinearPointer(UniqueIdentifier(id = uuid), IvnoTokenType::class.java)
)
val amountWithDifferentQuantity = amount.copy(quantity = BigDecimal.valueOf(13))
val amountWithDifferentAmountType =
    amount.copy(amountType = LinearPointer(UniqueIdentifier(id = anotherUuid), IvnoTokenType::class.java))

const val someString = "Prince"
const val anotherString = "Tafkap"
val someCordaX500Name = CordaX500Name.parse("O=tafkap,L=New York,C=US")
val anotherCordaX500Name = CordaX500Name.parse("O=prince,L=New York,C=US")

val network = Network(
    value = "Network 1",
    operator = party
)
val anotherNetworkWithDifferentValue = Network(
    value = "Network 2",
    operator = party
)
val anotherNetworkWithDifferentOperator = Network(
    value = "Network 1",
    operator = anotherParty
)

val ivnoTokenType = IvnoTokenType(
    network = network,
    custodian = party,
    tokenIssuingEntity = anotherParty,
    displayName = "Display Name 1",
    fractionDigits = 1,
    linearId = someUniqueIdentifier
)
val ivnoTokenTypeWithNetworkOfDifferentValue = ivnoTokenType.copy(network = anotherNetworkWithDifferentValue)
val ivnoTokenTypeWithNetworkOfDifferentOperator = ivnoTokenType.copy(network = anotherNetworkWithDifferentOperator)
val ivnoTokenTypeWithDifferentCustodian = ivnoTokenType.copy(custodian = anotherParty)
val ivnoTokenTypeWithDifferentTokenIssuingEntity = ivnoTokenType.copy(tokenIssuingEntity = party)
val ivnoTokenTypeWithDifferentDisplayName = ivnoTokenType.copy(displayName = "Display Name 2")
val ivnoTokenTypeWithDifferentFractionDigits = ivnoTokenType.copy(fractionDigits = 2)
val ivnoTokenTypeWithDifferentLinearId = ivnoTokenType.copy(linearId = anotherUniqueIdentifier)

val tokenDescriptor: TokenDescriptor = TokenDescriptor(someString, someCordaX500Name)
val anotherTokenDescriptor: TokenDescriptor = TokenDescriptor(anotherString, anotherCordaX500Name)
val tokenDescriptorWithOtherSymbol = TokenDescriptor(anotherString, someCordaX500Name)
val tokenDescriptorWithOtherName = TokenDescriptor(someString, anotherCordaX500Name)

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

val abstractClaimWithString: AbstractClaim<String> = Claim("Property 1", "Value 1")
val claimWithString = abstractClaimWithString as Claim<String>
val anotherAbstractClaimWithString: AbstractClaim<String> = Claim("Property 2", "Value 2")
val anotherClaimWithString = anotherAbstractClaimWithString as Claim<String>
val abstractClaimWithInt: AbstractClaim<Int> = Claim("Property 1", 1)
val claimWithInt = abstractClaimWithInt as Claim<Int>
val anotherAbstractClaimWithInt: AbstractClaim<Int> = Claim("Property 2", 2)
val anotherClaimWithInt = anotherAbstractClaimWithInt as Claim<Int>
val abstractClaimWithContextual: AbstractClaim<StateRef> = Claim("Property 1", stateRef)
val claimWithContextual = abstractClaimWithContextual as Claim<StateRef>
val anotherAbstractClaimWithContextual: AbstractClaim<StateRef> = Claim("Property 2", anotherStateRef)
val anotherClaimWithContextual = anotherAbstractClaimWithContextual as Claim<StateRef>
val abstractClaimWithPolymorphic: AbstractClaim<AbstractParty> = Claim("Property 1", party)
val claimWithPolymorphic = abstractClaimWithPolymorphic as Claim<AbstractParty>
val anotherAbstractClaimWithPolymorphic: AbstractClaim<AbstractParty> = Claim("Property 2", anotherParty)
val anotherClaimWithPolymorphic = anotherAbstractClaimWithPolymorphic as Claim<AbstractParty>

val stringClaimSet = setOf(Claim("Property 1", "Value 1"))
val anotherStringClaimSet = setOf(Claim("Property 1", "Value 1"), Claim("Property 1", "Value 2"))
val intClaimSet = setOf(Claim("Property 1", 1), Claim("Property 1", 2))

val stringSettingsSet = setOf(Setting("Property 1", "Value 1"))
val anotherStringSettingsSet = setOf(Setting("Property 1", "Value 1"), Setting("Property 2", "Value 2"))
val intSettingsSet = setOf(Setting("Property 1", 1), Setting("Property 2", 2))

val membershipWithString = Membership(
    network,
    party,
    stringClaimSet,
    stringSettingsSet,
    someUniqueIdentifier,
    StateRef(SecureHash.allOnesHash, 1)
)
val anotherMembershipWithString = membershipWithString.copy(holder = anotherParty)
val membershipWithInt = membershipWithString.copy(identity = intClaimSet, settings = intSettingsSet)
val anotherMembershipWithInt = membershipWithInt.copy(holder = anotherParty)
val membershipWithIntAndString = membershipWithInt.copy(settings = stringSettingsSet)
val anotherMembershipWithIntAndString = membershipWithIntAndString.copy(holder = anotherParty)

val membership = membershipWithString
val membershipWithNetworkOfDifferentValue = membership.copy(network = anotherNetworkWithDifferentValue)
val membershipWithNetworkOfDifferentOperator = membership.copy(network = anotherNetworkWithDifferentOperator)
val membershipWithDifferentHolder = membership.copy(holder = anotherParty)
val membershipWithDifferentIdentity = membership.copy(identity = anotherStringClaimSet)
val membershipWithDifferentSettings = membership.copy(settings = anotherStringSettingsSet)
val membershipWithDifferentLinearId = membership.copy(linearId = anotherUniqueIdentifier)
val membershipWithDifferentPreviousStateRef = membership.copy(previousStateRef = StateRef(SecureHash.zeroHash, 0))

@Serializable
data class MyContractState(override val participants: List<@Polymorphic AbstractParty>) : ContractState

@Serializable
data class MyLinearState(
    override val participants: List<@Polymorphic AbstractParty>,
    override val linearId: @Contextual UniqueIdentifier,
) : LinearState

val attestationPointer = AttestationPointer(
    stateRef = stateRef,
    stateClass = MyLinearState::class.java,
    stateLinearId = someUniqueIdentifier
)
val anotherAttestationPointer = AttestationPointer(
    stateRef = anotherStateRef,
    stateClass = MyContractState::class.java,
)

val attestationPointerWithDifferentStateRef = attestationPointer.copy { stateRef = anotherStateRef }
val attestationPointerWithDifferentStateLinearId = attestationPointer.copy { stateLinearId = anotherUniqueIdentifier }
val attestationPointerWithDifferentStateClass = attestationPointer.builder().withStateClass(
    MyContractState::class.java,
).build()
val membershipAttestationPointer = attestationPointerWithDifferentStateLinearId.builder().withStateClass(
    Membership::class.java
).build()

val nettedAccountAmount = NettedAccountAmount(
    AccountAddress(someString, someCordaX500Name),
    BigDecimalAmount(1, tokenDescriptor)
)
val anotherNettedAccountAmount = NettedAccountAmount(
    AccountAddress(anotherString, anotherCordaX500Name),
    BigDecimalAmount(42, anotherTokenDescriptor)
)
val nettedAccountAmountWithAccountAddressOfDifferentAccountId = nettedAccountAmount.copy(
    accountAddress = AccountAddress(anotherString, someCordaX500Name)
)
val nettedAccountAmountWithAccountAddressOfDifferentParty = nettedAccountAmount.copy(
    accountAddress = AccountAddress(someString, anotherCordaX500Name)
)
val nettedAccountAmountWithAmountOfDifferentQuantity= nettedAccountAmount.copy(
    amount = BigDecimalAmount(42, tokenDescriptor)
)
val nettedAccountAmountWithAmountOfDifferentAmountType= nettedAccountAmount.copy(
    amount = BigDecimalAmount(1, anotherTokenDescriptor)
)

val state = State(
    listOf(party),
    "A command",
    listOf(nettedAccountAmount),
    "A description",
    instant,
)
val anotherState = State(
    listOf(party, anotherParty),
    "Another command",
    listOf(nettedAccountAmount, anotherNettedAccountAmount),
    "Another description",
    anotherInstant,
    SecureHash.zeroHash
)
val stateWithDifferentParticipants = state.copy(participants = anotherState.participants)
val stateWithDifferentCommand = state.copy(command = anotherState.command)
val stateWithDifferentAmounts = state.copy(amounts = anotherState.amounts)
val stateWithDifferentDescription = state.copy(description = anotherState.description)
val stateWithDifferentTransactionTime = state.copy(transactionTime = anotherState.transactionTime)
val stateWithDifferentTransactionId = state.copy(transactionId = anotherState.transactionId)

val attestation = Attestation(
    attestor = party,
    attestees = setOf(anotherParty),
    pointer = membershipAttestationPointer,
    status = AttestationStatus.ACCEPTED,
    metadata = emptyMap(),
    linearId = someUniqueIdentifier,
    previousStateRef = stateRef,
)

val attestationWithDifferentAttestor = attestation.copy { attestor = anEvenOtherParty }
val attestationWithDifferentAttestees = attestation.copy { attestees = setOf(anEvenOtherParty) }
val attestationWithDifferentPointer = attestation.builder().withPointer(attestationPointer).build()
val attestationWithDifferentStatus = attestation.copy { status = AttestationStatus.REJECTED }
val attestationWithDifferentLinearId = attestation.copy { linearId = anotherUniqueIdentifier }
val attestationWithoutPreviousState = attestation.copy { previousStateRef = null }
val attestationWithDifferentPreviousState = attestation.copy { previousStateRef = anotherStateRef }

val membershipAttestation = MembershipAttestationSurrogate(
    network, attestation
).toOriginal()
val anotherMembershipAttestation = MembershipAttestationSurrogate(
    anotherNetworkWithDifferentOperator,
    attestation.copy {
        attestor = anEvenOtherParty
    }
).toOriginal()

class MyTokenContract: TokenContract()

val tokenContractCommandMove = TokenContract.Command.Move()
val tokenContractCommandMoveWithOtherContract = TokenContract.Command.Move(MyTokenContract::class.java)
val tokenContractCommandMoveWithoutContract = TokenContract.Command.Move(null)
