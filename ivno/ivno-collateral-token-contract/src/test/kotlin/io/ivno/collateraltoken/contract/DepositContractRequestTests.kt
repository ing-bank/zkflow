package io.ivno.collateraltoken.contract

import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Re-enable once we have everything serializable and when we have zktransaction DSL")
class DepositContractRequestTests : ContractTest() {

    /**
     * ING TEST
     */
    @Test
    fun `On deposit requesting, the transaction must include the Request command`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                fails()
                command(keysOf(BANK_A), DepositContract.Request())
                verifies()
            }
        }
    }

    @Test
    fun `On deposit requesting, zero deposit states must be consumed`() {
        services.ledger {
            transaction {
                input(DepositContract.ID, DEPOSIT)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_DEPOSIT_INPUTS)
            }
        }
    }

    @Test
    fun `On deposit requesting, only one deposit state must be created`() {
        services.ledger {
            transaction {
                output(DepositContract.ID, DEPOSIT)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_DEPOSIT_OUTPUTS)
            }
        }
    }

    @Test
    fun `On deposit requesting, only one Ivno token type must be referenced`() {
        services.ledger {
            transaction {
                output(DepositContract.ID, DEPOSIT)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_TOKEN_TYPE_REFERENCES)
            }
        }
    }

    @Test
    fun `On deposit requesting, a membership state must be referenced for each deposit participant (BANK_A missing)`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On deposit requesting, a membership state must be referenced for each deposit participant (CUSTODIAN missing)`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On deposit requesting, a membership state must be referenced for each deposit participant (TOKEN_ISSUING_ENTITY missing)`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On deposit requesting, a membership attestation state must be referenced for each deposit participant (BANK_A missing)`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On deposit requesting, a membership attestation state must be referenced for each deposit participant (CUSTODIAN missing)`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On deposit requesting, a membership attestation state must be referenced for each deposit participant (TOKEN_ISSUING_ENTITY missing)`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On deposit requesting, every membership attestation status must be ACCEPTED`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships(status = AttestationStatus.REJECTED)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS)
            }
        }
    }

    @Test
    fun `On deposit requesting, every membership's network must be equal to the Ivno token type network (BANK_A invalid)`() {
        services.ledger {
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
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On deposit requesting, every membership's network must be equal to the Ivno token type network (CUSTODIAN invalid)`() {
        services.ledger {
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
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On deposit requesting, every membership's network must be equal to the Ivno token type network (TOKEN_ISSUING_ENTITY invalid)`() {
        services.ledger {
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
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On deposit requesting, every membership attestation's network must be equal to the Ivno token type network (BANK_A invalid)`() {
        services.ledger {
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
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On deposit requesting, every membership attestation's network must be equal to the Ivno token type network (CUSTODIAN invalid)`() {
        services.ledger {
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
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On deposit requesting, every membership attestation's network must be equal to the Ivno token type network (TOKEN_ISSUING_ENTITY invalid)`() {
        services.ledger {
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
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On deposit requesting, every membership attestation state must point to a referenced membership state`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                val (bankMembership, bankAttestation) = createMembership(BANK_A.party, evolveMembership = true)
                reference(bankMembership.ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(bankAttestation.ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On deposit requesting, the depositor and the custodian must not be the same participant`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT.copy(custodian = BANK_A.party.anonymise()))
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_PARTICIPANTS)
            }
        }
    }

    @Test
    fun `On deposit requesting, the amount must be greater than zero`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT.copy(amount = AMOUNT_OF_ZERO_IVNO_TOKEN_POINTER))
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_AMOUNT)
            }
        }
    }

    @Test
    fun `On deposit requesting, the reference must be null`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT.copy(reference = "NOT NULL"))
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_REFERENCE)
            }
        }
    }

    @Test
    fun `On deposit requesting, the status must be DEPOSIT_REQUESTED`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT.copy(status = DepositStatus.DEPOSIT_CANCELLED))
                command(keysOf(BANK_A), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_STATUS)
            }
        }
    }

    @Test
    fun `On deposit requesting, the depositor must sign the transaction`() {
        services.ledger {
            transaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(TOKEN_ISSUING_ENTITY), DepositContract.Request())
                failsWith(DepositContract.Request.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
