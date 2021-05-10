package io.ivno.collateraltoken.contract

import io.dasl.contracts.v1.token.TokenContract
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test

class RedemptionRequestContractTests : ContractTest() {

    @Test
    fun `On redemption requesting, the transaction must include the Request command`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                fails()
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                verifies()
            }
        }
    }

    @Test
    fun `On redemption requesting, zero redemption states must be consumed`() {
        services.ledger {
            transaction {
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_REDEMPTION_INPUTS)
            }
        }
    }

    @Test
    fun `On redemption requesting, only one redemption state must be created`() {
        services.ledger {
            transaction {
                output(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_REDEMPTION_OUTPUTS)
            }
        }
    }

    @Test
    fun `On redemption requesting, at least one token state must be consumed`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_TOKEN_INPUTS)
            }
        }
    }

    @Test
    fun `On redemption requesting, only one Ivno token type must be referenced (verifies)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                verifies()
            }
        }
    }

    @Test
    fun `On redemption requesting, a membership state must be referenced for each redemption participant (BANK_A missing)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption requesting, a membership state must be referenced for each redemption participant (CUSTODIAN missing)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption requesting, a membership state must be referenced for each redemption participant (TOKEN_ISSUING_ENTITY missing)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption requesting, a membership attestation state must be referenced for each redemption participant (BANK_A missing)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption requesting, a membership attestation state must be referenced for each redemption participant (CUSTODIAN missing)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption requesting, a membership attestation state must be referenced for each redemption participant (TOKEN_ISSUING_ENTITY missing)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption requesting, every membership attestation status must be ACCEPTED`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships(status = AttestationStatus.REJECTED)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS)
            }
        }
    }

    @Test
    fun `On redemption requesting, every membership's network must be equal to the Ivno token type network (BANK_A invalid)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (invalidMembership, _) = createMembership(BANK_A.party, network = network)
                reference(invalidMembership.ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption requesting, every membership's network must be equal to the Ivno token type network (CUSTODIAN invalid)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (invalidMembership, _) = createMembership(CUSTODIAN.party, network = network)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(invalidMembership.ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption requesting, every membership's network must be equal to the Ivno token type network (TOKEN_ISSUING_ENTITY invalid)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (invalidMembership, _) = createMembership(TOKEN_ISSUING_ENTITY.party, network = network)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(invalidMembership.ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption requesting, every membership attestation's network must be equal to the Ivno token type network (BANK_A invalid)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (_, invalidAttestation) = createMembership(BANK_A.party, network = network)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(invalidAttestation.ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption requesting, every membership attestation's network must be equal to the Ivno token type network (CUSTODIAN invalid)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (_, invalidAttestation) = createMembership(CUSTODIAN.party, network = network)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(invalidAttestation.ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption requesting, every membership attestation's network must be equal to the Ivno token type network (TOKEN_ISSUING_ENTITY invalid)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val network = Network("INVALID_NETWORK", BNO.party)
                val memberships = createAllMemberships()
                val (_, invalidAttestation) = createMembership(TOKEN_ISSUING_ENTITY.party, network = network)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(invalidAttestation.ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption requesting, every membership attestation state must point to a referenced membership state`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                val (bankMembership, bankAttestation) = createMembership(BANK_A.party, evolveMembership = true)
                reference(bankMembership.ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(bankAttestation.ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption requesting, the depositor, custodian and token issuing entity must be different`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION.copy(redeemer = CUSTODIAN.party))
                reference(tokenType.ref)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_PARTICIPANTS)
            }
        }
    }

    @Test
    fun `On redemption requesting, the redemption amount must be equal to the sum of the input token amount (not enough)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION.copy(amount = AMOUNT_OF_200_IVNO_TOKEN_POINTER))
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_AMOUNT)
            }
        }
    }

    @Test
    fun `On redemption requesting, the redemption amount must be equal to the sum of the input token amount (too much)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(tokenType.ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION.copy(amount = AMOUNT_OF_50_IVNO_TOKEN_POINTER))
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_AMOUNT)
            }
        }
    }

    @Test
    fun `On redemption requesting, all participants must sign the transaction (redeemer must sign)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                reference(tokenType.ref)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `On redemption requesting, all participants must sign the transaction (custodian must sign)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                reference(tokenType.ref)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `On redemption requesting, all participants must sign the transaction (token issuing entity must sign)`() {
        services.ledger {
            val tokenType = issueTokenType(IVNO_TOKEN_TYPE)
            val requestedDeposit = requestDeposit(DEPOSIT, tokenType)
            val acceptedDeposit = acceptDeposit(requestedDeposit, tokenType)
            val paymentIssuedDeposit = issueDepositPayment(acceptedDeposit, tokenType)
            val token = acceptDepositPayment(paymentIssuedDeposit, tokenType)
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                input(token.ref)
                output(RedemptionContract.ID, REDEMPTION)
                reference(tokenType.ref)
                command(keysOf(BANK_A, TOKEN_ISSUING_ENTITY), TokenContract.Command.Redeem)
                command(keysOf(BANK_A, CUSTODIAN), RedemptionContract.Request)
                failsWith(RedemptionContract.Request.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
