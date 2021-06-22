package io.ivno.collateraltoken.contract

import com.ing.zknotary.testing.dsl.zkLedger
import io.dasl.contracts.v1.crud.CrudCommands
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Role
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Re-enable once we have everything serializable and when we have zktransaction DSL")
class IvnoTokenTypeContractCreationTests : ContractTest() {

    @Test
    fun `On token type creation, the transaction must include the Create command`() {
        services.zkLedger {
            zkTransaction {
                val roles = setOf(Role("TOKEN_ISSUING_ENTITY"))
                val (membership, attestation) = createMembership(TOKEN_ISSUING_ENTITY.party, roles = roles)
                reference(membership.ref)
                reference(attestation.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                fails()
                command(keysOf(TOKEN_ISSUING_ENTITY), CrudCommands.Create)
                verifies()
            }
        }
    }

    @Test
    fun `On Ivno token type creating, a membership state must be referenced for the token issuing entity`() {
        services.zkLedger {
            zkTransaction {
                val roles = setOf(Role("TOKEN_ISSUING_ENTITY"))
                val (_, attestation) = createMembership(TOKEN_ISSUING_ENTITY.party, roles = roles)
                reference(attestation.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                command(keysOf(TOKEN_ISSUING_ENTITY), CrudCommands.Create)
                failsWith(IvnoTokenTypeContract.Create.CONTRACT_RULE_MEMBERSHIP_REFERENCE)
            }
        }
    }

    @Test
    fun `On Ivno token type creating, a membership state must be referenced for the token issuing entity (Invalid TIE on TT)`() {
        services.zkLedger {
            zkTransaction {
                val roles = setOf(Role("TOKEN_ISSUING_ENTITY"))
                val (membership, attestation) = createMembership(TOKEN_ISSUING_ENTITY.party, roles = roles)
                reference(membership.ref)
                reference(attestation.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE.copy(tokenIssuingEntity = BANK_A.party))
                command(keysOf(TOKEN_ISSUING_ENTITY), CrudCommands.Create)
                failsWith(IvnoTokenTypeContract.Create.CONTRACT_RULE_MEMBERSHIP_REFERENCE)
            }
        }
    }

    @Test
    fun `On Ivno token type creating, a membership attestation state must be referenced for the token issuing entity`() {
        services.zkLedger {
            zkTransaction {
                val roles = setOf(Role("TOKEN_ISSUING_ENTITY"))
                val (membership, _) = createMembership(TOKEN_ISSUING_ENTITY.party, roles = roles)
                reference(membership.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                command(keysOf(TOKEN_ISSUING_ENTITY), CrudCommands.Create)
                failsWith(IvnoTokenTypeContract.Create.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_REFERENCE)
            }
        }
    }

    @Test
    fun `On Ivno token type creating, the referenced membership attestation state must point to the referenced membership state`() {
        services.zkLedger {
            zkTransaction {
                val (membership, attestation) = createMembership(
                    TOKEN_ISSUING_ENTITY.party,
                    roles = setOf(Role("TOKEN_ISSUING_ENTITY")),
                    evolveMembership = true
                )
                reference(membership.ref)
                reference(attestation.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                command(keysOf(TOKEN_ISSUING_ENTITY), CrudCommands.Create)
                failsWith(IvnoTokenTypeContract.Create.CONTRACT_RULE_ATTESTATION_POINTS_TO_MEMBERSHIP)
            }
        }
    }

    @Test
    fun `On Ivno token type creating, the referenced membership state must possess the TOKEN_ISSUING_ENTITY role`() {
        services.zkLedger {
            zkTransaction {
                val (membership, attestation) = createMembership(TOKEN_ISSUING_ENTITY.party)
                reference(membership.ref)
                reference(attestation.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                command(keysOf(TOKEN_ISSUING_ENTITY), CrudCommands.Create)
                failsWith(IvnoTokenTypeContract.Create.CONTRACT_RULE_MEMBERSHIP_ROLES)
            }
        }
    }

    @Test
    fun `On Ivno token type creating, the membership attestation status must be ACCEPTED`() {
        services.zkLedger {
            zkTransaction {
                val roles = setOf(Role("TOKEN_ISSUING_ENTITY"))
                val (membership, attestation) = createMembership(
                    TOKEN_ISSUING_ENTITY.party,
                    roles = roles,
                    status = AttestationStatus.REJECTED
                )
                reference(membership.ref)
                reference(attestation.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                command(keysOf(TOKEN_ISSUING_ENTITY), CrudCommands.Create)
                failsWith(IvnoTokenTypeContract.Create.CONTRACT_RULE_MEMBERSHIP_ATTESTATION_STATUS)
            }
        }
    }

    @Test
    fun `On Ivno token type creating, the issued token type network must be equal to the membership network`() {
        services.zkLedger {
            zkTransaction {
                val roles = setOf(Role("TOKEN_ISSUING_ENTITY"))
                val (membership, attestation) = createMembership(TOKEN_ISSUING_ENTITY.party, roles = roles)
                reference(membership.ref)
                reference(attestation.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE.copy(network = Network("Invalid Network")))
                command(keysOf(TOKEN_ISSUING_ENTITY), CrudCommands.Create)
                failsWith(IvnoTokenTypeContract.Create.CONTRACT_RULE_MEMBERSHIP_AND_TOKEN_TYPE_NETWORK)
            }
        }
    }

    @Test
    fun `On Ivno token type creating, the token issuing entity must sign the transaction`() {
        services.zkLedger {
            zkTransaction {
                val roles = setOf(Role("TOKEN_ISSUING_ENTITY"))
                val (membership, attestation) = createMembership(TOKEN_ISSUING_ENTITY.party, roles = roles)
                reference(membership.ref)
                reference(attestation.ref)
                output(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                command(keysOf(BANK_A), CrudCommands.Create)
                failsWith(IvnoTokenTypeContract.Create.CONTRACT_RULE_SIGNERS)
            }
        }
    }
}
