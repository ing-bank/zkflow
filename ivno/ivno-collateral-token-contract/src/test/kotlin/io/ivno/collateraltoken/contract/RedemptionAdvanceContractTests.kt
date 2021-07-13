package io.ivno.collateraltoken.contract

import com.ing.zknotary.testing.dsl.zkLedger
import io.dasl.contracts.v1.token.TokenContract
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import net.corda.core.contracts.UniqueIdentifier
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Disabled("Re-enable once we have everything serializable and when we have zktransaction DSL")
class RedemptionAdvanceContractTests : ContractTest() {

    @Test
    fun `On redemption advancing, the transaction must include the Advance command`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                fails()
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                verifies()
            }
        }
    }

    @Test
    fun `On redemption advancing, only one redemption state must be consumed`() {
        services.zkLedger {
            zkTransaction {
                input(RedemptionContract.ID, REDEMPTION)
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_REDEMPTION_INPUTS)
            }
        }
    }

    @Test
    fun `On redemption advancing, only one redemption state must be created`() {
        services.zkLedger {
            zkTransaction {
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_REDEMPTION_OUTPUTS)
            }
        }
    }

    @Test
    fun `On redemption advancing, only one token must be created when the advance status is REJECTED`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.rejectRedemption())
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_A)
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_A)
                command(keysOf(CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Advance)
                command(keysOf(TOKEN_ISSUING_ENTITY), TokenContract.Command.Issue)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_TOKEN_OUTPUTS)
            }
        }
    }

    @Test
    fun `On redemption advancing, only one Ivno token type must be referenced`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_TOKEN_TYPE_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption advancing, a membership state must be referenced for each redemption participant (BANK_A missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption advancing, a membership state must be referenced for each redemption participant (CUSTODIAN missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption advancing, a membership state must be referenced for each redemption participant (TOKEN_ISSUING_ENTITY missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption advancing, a membership attestation state must be referenced for each redemption participant (BANK_A missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption advancing, a membership attestation state must be referenced for each redemption participant (CUSTODIAN missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption advancing, a membership attestation state must be referenced for each redemption participant (TOKEN_ISSUING_ENTITY missing)`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption advancing, every membership attestation status must be ACCEPTED`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS)
            }
        }
    }

    @Test
    fun `On redemption advancing, every membership's network must be equal to the Ivno token type network (BANK_A invalid)`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption advancing, every membership's network must be equal to the Ivno token type network (CUSTODIAN invalid)`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption advancing, every membership's network must be equal to the Ivno token type network (TOKEN_ISSUING_ENTITY invalid)`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption advancing, every membership attestation's network must be equal to the Ivno token type network (BANK_A invalid)`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption advancing, every membership attestation's network must be equal to the Ivno token type network (CUSTODIAN invalid)`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption advancing, every membership attestation's network must be equal to the Ivno token type network (TOKEN_ISSUING_ENTITY invalid)`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_NETWORK)
            }
        }
    }

    @Test
    fun `On redemption advancing, every membership attestation state must point to a referenced membership state`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.completeRedemption())
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_MEMBERSHIP_ATTESTATIONS_POINT_TO_MEMBERSHIP_REFERENCES)
            }
        }
    }

    @Test
    fun `On redemption advancing, the redeemer, custodian, token issuing entity, amount and linearId must not change`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.rejectRedemption().copy(linearId = UniqueIdentifier()))
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_A)
                command(keysOf(CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Advance)
                command(keysOf(TOKEN_ISSUING_ENTITY), TokenContract.Command.Issue)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_CHANGES)
            }
        }
    }

    @Test
    fun `On redemption advancing, the output state must be able to advance from the input state`() {
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
                input(RedemptionContract.ID, REDEMPTION.rejectRedemption())
                output(RedemptionContract.ID, REDEMPTION)
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_A)
                command(keysOf(CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Advance)
                command(keysOf(TOKEN_ISSUING_ENTITY), TokenContract.Command.Issue)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_CAN_ADVANCE)
            }
        }
    }

    @Test
    fun `On redemption advancing, the token output amount must be equal to the redemption amount when the advance status is REJECTED`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.rejectRedemption())
                output(TokenContract.CONTRACT_ID, TOKEN_50GBP_BANK_A)
                command(keysOf(CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Advance)
                command(keysOf(TOKEN_ISSUING_ENTITY), TokenContract.Command.Issue)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_TOKEN_AMOUNT)
            }
        }
    }

    @Test
    fun `On redemption advancing, the token output holder must be equal to the redemption redeemer when the advance status is REJECTED`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.rejectRedemption())
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_B)
                command(keysOf(CUSTODIAN, TOKEN_ISSUING_ENTITY), RedemptionContract.Advance)
                command(keysOf(TOKEN_ISSUING_ENTITY), TokenContract.Command.Issue)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_TOKEN_HOLDER)
            }
        }
    }

    @Test
    fun `On redemption advancing, the required signing participants must sign the transaction (custodian must sign)`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.rejectRedemption())
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_A)
                command(keysOf(TOKEN_ISSUING_ENTITY), RedemptionContract.Advance)
                command(keysOf(TOKEN_ISSUING_ENTITY), TokenContract.Command.Issue)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_SIGNERS)
            }
        }
    }

    @Test
    fun `On redemption advancing, the required signing participants must sign the transaction (token issuing entity must sign)`() {
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
                input(RedemptionContract.ID, REDEMPTION)
                output(RedemptionContract.ID, REDEMPTION.rejectRedemption())
                output(TokenContract.CONTRACT_ID, TOKEN_100GBP_BANK_A)
                command(keysOf(CUSTODIAN), RedemptionContract.Advance)
                command(keysOf(TOKEN_ISSUING_ENTITY), TokenContract.Command.Issue)
                failsWith(RedemptionContract.Advance.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
