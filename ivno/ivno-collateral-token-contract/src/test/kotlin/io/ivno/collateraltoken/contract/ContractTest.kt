package io.ivno.collateraltoken.contract

import TestTransactionDSLInterpreter
import TestZKLedgerDSLInterpreter
import TestZKTransactionDSLInterpreter
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.ZincZKTransactionService
import com.ing.zkflow.testing.dsl.LedgerDSL
import com.ing.zkflow.testing.dsl.VerificationMode
import com.ing.zkflow.testing.dsl.zkLedger
import io.dasl.contracts.v1.crud.CrudCommands
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenContract
import io.dasl.contracts.v1.token.TokenState
import io.dasl.contracts.v1.token.linearPointer
import io.dasl.contracts.v1.token.toBigDecimalAmount
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Role
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestationContract
import io.onixlabs.corda.bnms.contract.membership.MembershipContract
import io.onixlabs.corda.bnms.contract.membership.attest
import io.onixlabs.corda.bnms.contract.membership.getNextOutput
import io.onixlabs.corda.bnms.contract.relationship.RelationshipAttestationContract
import io.onixlabs.corda.bnms.contract.relationship.RelationshipContract
import io.onixlabs.corda.bnms.contract.revocation.RevocationLockContract
import io.onixlabs.corda.identityframework.contract.AttestationContract
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import io.onixlabs.corda.identityframework.contract.Claim
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NotaryInfo
import net.corda.core.utilities.loggerFor
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
abstract class ContractTest {
    private val myLog = loggerFor<ContractTest>()
    abstract val verificationMode: VerificationMode
    abstract val commandData: CommandData

    protected companion object {

        val BNO = TestIdentity(CordaX500Name("BNO", "London", "GB"))
        val BANK_A = TestIdentity(CordaX500Name("Bank A", "London", "GB"))
        val BANK_B = TestIdentity(CordaX500Name("Bank B", "London", "GB"))
        val TOKEN_ISSUING_ENTITY = TestIdentity(CordaX500Name("Token Issuing Entity", "Paris", "FR"))
        val CUSTODIAN = TestIdentity(CordaX500Name("Custodian", "New York", "US"))
        val RANDOM_PARTY = TestIdentity(CordaX500Name("Random Party", "Berlin", "DE"))

        val NETWORK = Network("Ivno", BNO.party)
        val IVNO_TOKEN_TYPE = IvnoTokenType(NETWORK, CUSTODIAN.party, TOKEN_ISSUING_ENTITY.party, "GBP", 2)
        val IVNO_TOKEN_TYPE_POINTER = IVNO_TOKEN_TYPE.toPointer()
        val DUMMY_TOKEN_TYPE = IvnoTokenType(NETWORK, BANK_A.party, BANK_B.party, "DUMMY", 2)

        val AMOUNT_OF_50_IVNO_TOKEN_POINTER = BigDecimalAmount(50.toBigDecimal(), IVNO_TOKEN_TYPE_POINTER)
        val AMOUNT_OF_100_IVNO_TOKEN_POINTER = BigDecimalAmount(100.toBigDecimal(), IVNO_TOKEN_TYPE_POINTER)
        val AMOUNT_OF_200_IVNO_TOKEN_POINTER = BigDecimalAmount(200.toBigDecimal(), IVNO_TOKEN_TYPE_POINTER)

        val AMOUNT_OF_ZERO_IVNO_TOKEN_POINTER = BigDecimalAmount(BigDecimal.ZERO, IVNO_TOKEN_TYPE_POINTER)

        val TOKEN_100GBP_BANK_A = TokenState(
            accountId = "12345678",
            amount = "100".toBigDecimalAmount(IVNO_TOKEN_TYPE),
            tokenTypePointer = IVNO_TOKEN_TYPE.linearPointer(),
            issuer = TOKEN_ISSUING_ENTITY.party,
            owner = BANK_A.party
        )

        val TOKEN_50GBP_BANK_A = TokenState(
            accountId = "12345678",
            amount = "50".toBigDecimalAmount(IVNO_TOKEN_TYPE),
            tokenTypePointer = IVNO_TOKEN_TYPE.linearPointer(),
            issuer = TOKEN_ISSUING_ENTITY.party,
            owner = BANK_A.party
        )

        val TOKEN_100GBP_BANK_B = TokenState(
            accountId = "87654321",
            amount = "100".toBigDecimalAmount(IVNO_TOKEN_TYPE),
            tokenTypePointer = IVNO_TOKEN_TYPE.linearPointer(),
            issuer = TOKEN_ISSUING_ENTITY.party,
            owner = BANK_B.party
        )

        val TOKEN_50GBP_BANK_B = TokenState(
            accountId = "87654321",
            amount = "50".toBigDecimalAmount(IVNO_TOKEN_TYPE),
            tokenTypePointer = IVNO_TOKEN_TYPE.linearPointer(),
            issuer = TOKEN_ISSUING_ENTITY.party,
            owner = BANK_B.party
        )

        val DEPOSIT = Deposit(
            BANK_A.party.anonymise(),
            CUSTODIAN.party.anonymise(),
            TOKEN_ISSUING_ENTITY.party.anonymise(),
            AMOUNT_OF_100_IVNO_TOKEN_POINTER,
            "12345678"
        )

        val TRANSFER = Transfer(
            BANK_A.party.anonymise(),
            BANK_B.party.anonymise(),
            TransferInitiator.CURRENT_HOLDER,
            AMOUNT_OF_100_IVNO_TOKEN_POINTER,
            "12345678",
            "87654321"
        )

        val REDEMPTION = Redemption(
            BANK_A.party,
            CUSTODIAN.party,
            TOKEN_ISSUING_ENTITY.party,
            AMOUNT_OF_100_IVNO_TOKEN_POINTER,
            "12345678"
        )

        private val cordapps = listOf(
            "io.ivno.collateraltoken.contract",
            "io.onixlabs.corda.identityframework.contract",
            "io.onixlabs.corda.bnms.contract",
            "io.dasl.contracts.v1"
        )

        private val contracts = listOf(
            DepositContract.ID,
            TransferContract.ID,
            RedemptionContract.ID,
            IvnoTokenTypeContract.ID,
            TokenContract.CONTRACT_ID,
            MembershipContract.ID,
            RelationshipContract.ID,
            RevocationLockContract.ID,
            MembershipAttestationContract.ID,
            RelationshipAttestationContract.ID
        )

        fun keysOf(vararg identities: TestIdentity) = identities.map { it.publicKey }
    }

    private lateinit var _services: MockServices
    protected val services: MockServices get() = _services

    @BeforeEach
    private fun setup() {
        val networkParameters = testNetworkParameters(
            minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION,
            notaries = listOf(NotaryInfo(TestIdentity(DUMMY_NOTARY_NAME, 20).party, true))
        )

        _services = MockServices(cordapps, BANK_A, networkParameters, BANK_B)
        contracts.forEach { _services.addMockCordapp(it) }

        if (verificationMode == VerificationMode.PROVE_AND_VERIFY
            && commandData is ZKTransactionMetadataCommandData
        ) {
            _services.zkLedger {
                val zkService = this.interpreter.zkService as ZincZKTransactionService
                val time = measureTime {
                    zkService.setup(commandData as ZKTransactionMetadataCommandData)
                }
                myLog.info("[setup] $time")
            }
        }
    }

    protected fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.createMembership(
        holder: AbstractParty,
        attestor: AbstractParty = BNO.party,
        network: Network = NETWORK,
        status: AttestationStatus = AttestationStatus.ACCEPTED,
        roles: Set<Role> = emptySet(),
        evolveMembership: Boolean = false
    ): Pair<StateAndRef<Membership>, StateAndRef<MembershipAttestation>> {
        val membershipLabel = SecureHash.randomSHA256().toString()
        val attestationLabel = SecureHash.randomSHA256().toString()
        // TODO: Fix the need for setting identity and settings sets for BFL
        val membership = Membership(network, holder, setOf(Claim("DUMMY", 1)))
            .addRoles(*roles.toTypedArray())
            .addSetting("Dummy", 1)

        transaction {
            output(MembershipContract.ID, membershipLabel, membership)
            command(listOf(membership.holder.owningKey), MembershipContract.Issue)
            verifies()
        }

        val issuedMembership = retrieveOutputStateAndRef(Membership::class.java, membershipLabel)
        val attestation = issuedMembership.attest(attestor, status)

        transaction {
            output(MembershipAttestationContract.ID, attestationLabel, attestation)
            reference(issuedMembership.ref)
            command(listOf(attestation.attestor.owningKey), AttestationContract.Issue)
            verifies()
        }

        val issuedAttestation = retrieveOutputStateAndRef(MembershipAttestation::class.java, attestationLabel)

        if (evolveMembership) {
            val evolvedMembership = issuedMembership.getNextOutput()
            val evolvedMembershipLabel = SecureHash.randomSHA256().toString()

            transaction {
                input(issuedMembership.ref)
                output(MembershipContract.ID, evolvedMembershipLabel, evolvedMembership)
                command(listOf(membership.holder.owningKey), MembershipContract.Amend)
                verifies()
            }

            val issuedEvolvedMembership = retrieveOutputStateAndRef(Membership::class.java, evolvedMembershipLabel)

            return issuedEvolvedMembership to issuedAttestation
        }

        return issuedMembership to issuedAttestation
    }

    protected fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.createAllMemberships(
        network: Network = NETWORK,
        status: AttestationStatus = AttestationStatus.ACCEPTED
    ): Map<StateAndRef<Membership>, StateAndRef<MembershipAttestation>> {
        return listOf(BNO, BANK_A, BANK_B, TOKEN_ISSUING_ENTITY, CUSTODIAN, RANDOM_PARTY).map {
            createMembership(it.party, network = network, status = status)
        }.toMap()
    }

    protected fun Map<StateAndRef<Membership>, StateAndRef<MembershipAttestation>>.membershipFor(
        identity: TestIdentity
    ): StateAndRef<Membership> = keys.single { it.state.data.holder == identity.party }

    protected fun Map<StateAndRef<Membership>, StateAndRef<MembershipAttestation>>.attestationFor(
        identity: TestIdentity
    ): StateAndRef<MembershipAttestation> = values.single { it.state.data.holder == identity.party }

    protected fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.issueTokenType(
        tokenType: IvnoTokenType
    ): StateAndRef<IvnoTokenType> {
        val label = SecureHash.randomSHA256().toString()

        transaction {
            val (membership, attestation) = createMembership(
                TOKEN_ISSUING_ENTITY.party,
                roles = setOf(Role("TOKEN_ISSUING_ENTITY"))
            )
            reference(membership.ref)
            reference(attestation.ref)
            output(IvnoTokenTypeContract.ID, label, tokenType)
            command(listOf(tokenType.tokenIssuingEntity.owningKey), CrudCommands.Create)
            verifies()
        }

        return retrieveOutputStateAndRef(IvnoTokenType::class.java, label)
    }

    fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.requestDeposit(
        deposit: Deposit,
        tokenType: StateAndRef<IvnoTokenType>
    ): StateAndRef<Deposit> {
        val label = SecureHash.randomSHA256().toString()

        zkTransaction {
            val memberships = createAllMemberships()
            reference(memberships.membershipFor(BANK_A).ref)
            reference(memberships.membershipFor(CUSTODIAN).ref)
            reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
            reference(memberships.attestationFor(BANK_A).ref)
            reference(memberships.attestationFor(CUSTODIAN).ref)
            reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
            reference(tokenType.ref)
            output(DepositContract.ID, label, deposit)
            command(listOf(deposit.depositor.owningKey), DepositContract.Request)
            verifies()
        }

        return retrieveOutputStateAndRef(Deposit::class.java, label)
    }

    fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.acceptDeposit(
        deposit: StateAndRef<Deposit>,
        tokenType: StateAndRef<IvnoTokenType>
    ): StateAndRef<Deposit> {
        val label = SecureHash.randomSHA256().toString()

        zkTransaction {
            val memberships = createAllMemberships()
            reference(memberships.membershipFor(BANK_A).ref)
            reference(memberships.membershipFor(CUSTODIAN).ref)
            reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
            reference(memberships.attestationFor(BANK_A).ref)
            reference(memberships.attestationFor(CUSTODIAN).ref)
            reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
            reference(tokenType.ref)
            input(deposit.ref)
            output(DepositContract.ID, label, deposit.state.data.acceptDeposit("REF"))
            command(listOf(deposit.state.data.custodian.owningKey), DepositContract.Advance)
            verifies()
        }

        return retrieveOutputStateAndRef(Deposit::class.java, label)
    }

    fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.issueDepositPayment(
        deposit: StateAndRef<Deposit>,
        tokenType: StateAndRef<IvnoTokenType>
    ): StateAndRef<Deposit> {
        val label = SecureHash.randomSHA256().toString()

        zkTransaction {
            val memberships = createAllMemberships()
            reference(memberships.membershipFor(BANK_A).ref)
            reference(memberships.membershipFor(CUSTODIAN).ref)
            reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
            reference(memberships.attestationFor(BANK_A).ref)
            reference(memberships.attestationFor(CUSTODIAN).ref)
            reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
            reference(tokenType.ref)
            input(deposit.ref)
            output(DepositContract.ID, label, deposit.state.data.issuePayment())
            command(listOf(deposit.state.data.depositor.owningKey), DepositContract.Advance)
            verifies()
        }

        return retrieveOutputStateAndRef(Deposit::class.java, label)
    }

    // FIXME: This transaction has multiple commands.
    //  Do they (all) need to be private (ZKP)?
    fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.acceptDepositPayment(
        deposit: StateAndRef<Deposit>,
        tokenType: StateAndRef<IvnoTokenType>
    ): StateAndRef<TokenState> {
        val label = SecureHash.randomSHA256().toString()

        transaction {
            val memberships = createAllMemberships()
            reference(memberships.membershipFor(BANK_A).ref)
            reference(memberships.membershipFor(CUSTODIAN).ref)
            reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
            reference(memberships.attestationFor(BANK_A).ref)
            reference(memberships.attestationFor(CUSTODIAN).ref)
            reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
            input(deposit.ref)
            output(DepositContract.ID, deposit.state.data.acceptPayment())
            output(TokenContract.CONTRACT_ID, label, TOKEN_100GBP_BANK_A)
            reference(tokenType.ref)
            command(listOf(tokenType.state.data.tokenIssuingEntity.owningKey), CrudCommands.Create)
            command(listOf(deposit.state.data.depositor.owningKey), TokenContract.Command.Issue)
            command(
                listOf(deposit.state.data.tokenIssuingEntity.owningKey, deposit.state.data.custodian.owningKey),
                DepositContract.Advance
            )
            verifies()
        }

        return retrieveOutputStateAndRef(TokenState::class.java, label)
    }

    fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.requestTransfer(
        transfer: Transfer,
        tokenType: StateAndRef<IvnoTokenType>
    ): StateAndRef<Transfer> {
        val label = SecureHash.randomSHA256().toString()

        transaction {
            val memberships = createAllMemberships()
            reference(memberships.membershipFor(BANK_A).ref)
            reference(memberships.membershipFor(BANK_B).ref)
            reference(memberships.attestationFor(BANK_A).ref)
            reference(memberships.attestationFor(BANK_B).ref)
            reference(tokenType.ref)
            output(TransferContract.ID, label, transfer)
            command(listOf(transfer.currentTokenHolder.owningKey), TransferContract.Request)
            verifies()
        }

        return retrieveOutputStateAndRef(Transfer::class.java, label)
    }

    fun LedgerDSL<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter,  TestZKLedgerDSLInterpreter>.acceptTransfer(
        transfer: StateAndRef<Transfer>,
        tokenType: StateAndRef<IvnoTokenType>
    ): StateAndRef<Transfer> {
        val label = SecureHash.randomSHA256().toString()

        transaction {
            val memberships = createAllMemberships()
            reference(memberships.membershipFor(BANK_A).ref)
            reference(memberships.membershipFor(BANK_B).ref)
            reference(memberships.attestationFor(BANK_A).ref)
            reference(memberships.attestationFor(BANK_B).ref)
            reference(tokenType.ref)
            input(transfer.ref)
            output(TransferContract.ID, label, transfer.state.data.acceptTransfer())
            command(listOf(transfer.state.data.targetTokenHolder.owningKey), TransferContract.Advance)
            verifies()
        }

        return retrieveOutputStateAndRef(Transfer::class.java, label)
    }
}
