package io.ivno.collateraltoken.contract

import com.ing.zknotary.testing.dsl.VerificationMode
import com.ing.zknotary.testing.dsl.zkLedger
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DepositContractRequestTests : ContractTest() {
    override val verificationMode = VerificationMode.RUN
    override val commandData = DepositContract.Request

    /**
     * ING TEST
     */
    @ExperimentalTime
    @Test
    fun `On deposit requesting, the transaction must include the Request command`() {
        // services.zkLedger(zkService = MockZKTransactionService(services)) {
        services.zkLedger {
            zkTransaction {
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
                command(keysOf(BANK_A), commandData)
                verifies(verificationMode)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, zero deposit states must be consumed`() {
        services.zkLedger {
            zkTransaction {
                input(DepositContract.ID, DEPOSIT)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_DEPOSIT_INPUTS)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, only one deposit state must be created`() {
        services.zkLedger {
            zkTransaction {
                output(DepositContract.ID, DEPOSIT)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_DEPOSIT_OUTPUTS)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, only one Ivno token type must be referenced`() {
        services.zkLedger {
            zkTransaction {
                output(DepositContract.ID, DEPOSIT)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_TOKEN_TYPE_REFERENCES)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, a membership state must be referenced for each deposit participant (BANK_A missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, a membership state must be referenced for each deposit participant (CUSTODIAN missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, a membership state must be referenced for each deposit participant (TOKEN_ISSUING_ENTITY missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, a membership attestation state must be referenced for each deposit participant (BANK_A missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, a membership attestation state must be referenced for each deposit participant (CUSTODIAN missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, a membership attestation state must be referenced for each deposit participant (TOKEN_ISSUING_ENTITY missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, every membership attestation status must be ACCEPTED`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships(status = AttestationStatus.REJECTED)
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, every membership's network must be equal to the Ivno token type network (BANK_A invalid)`() {
        services.zkLedger {
            zkTransaction {
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
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, every membership's network must be equal to the Ivno token type network (CUSTODIAN invalid)`() {
        services.zkLedger {
            zkTransaction {
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
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, every membership's network must be equal to the Ivno token type network (TOKEN_ISSUING_ENTITY invalid)`() {
        services.zkLedger {
            zkTransaction {
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
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, every membership attestation's network must be equal to the Ivno token type network (BANK_A invalid)`() {
        services.zkLedger {
            zkTransaction {
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
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, every membership attestation's network must be equal to the Ivno token type network (CUSTODIAN invalid)`() {
        services.zkLedger {
            zkTransaction {
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
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, every membership attestation's network must be equal to the Ivno token type network (TOKEN_ISSUING_ENTITY invalid)`() {
        services.zkLedger {
            zkTransaction {
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
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, every membership attestation state must point to a referenced membership state`() {
        services.zkLedger {
            zkTransaction {
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
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, the depositor and the custodian must not be the same participant`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT.copy(custodian = BANK_A.party.anonymise()))
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_PARTICIPANTS)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, the amount must be greater than zero`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT.copy(amount = AMOUNT_OF_ZERO_IVNO_TOKEN_POINTER))
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_AMOUNT)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, the reference must be null`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT.copy(reference = "NOT NULL"))
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_REFERENCE)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, the status must be DEPOSIT_REQUESTED`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT.copy(status = DepositStatus.DEPOSIT_CANCELLED))
                command(keysOf(BANK_A), commandData)
                failsWith(commandData.CONTRACT_RULE_STATUS)
            }
        }
    }

    @Test
    @Disabled
    fun `On deposit requesting, the depositor must sign the transaction`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                command(keysOf(TOKEN_ISSUING_ENTITY), commandData)
                failsWith(commandData.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
