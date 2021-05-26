package io.ivno.collateraltoken.workflow

import co.paralleluniverse.fibers.Suspendable
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import io.onixlabs.corda.bnms.workflow.membership.FindMembershipAttestationFlow
import io.onixlabs.corda.bnms.workflow.membership.FindMembershipFlow
import io.onixlabs.corda.bnms.workflow.membership.SynchronizeMembershipFlow
import io.onixlabs.corda.bnms.workflow.membership.SynchronizeMembershipFlowHandler
import io.onixlabs.corda.core.workflow.currentStep
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

@Suspendable
fun FlowLogic<*>.synchronizeMembership(
    network: Network,
    state: ContractState,
    sessions: Iterable<FlowSession>
): Set<ReferencedStateAndRef<*>> {
    val result = mutableSetOf<ReferencedStateAndRef<*>>()

    sessions.forEach { it.send(it.counterparty in state.participants) }

    state.participants.forEach {
        val (membership, attestation) = if (it in serviceHub.myInfo.legalIdentities) {
            resolveOurMembershipAndAttestation(network, it)
        } else {
            resolveTheirMembershipAndAttestation(network, sessions.single { session -> session.counterparty == it })
        }

        result.add(membership.referenced())
        result.add(attestation.referenced())
    }

    return result
}

@Suspendable
fun FlowLogic<*>.synchronizeMembershipHandler(session: FlowSession) {
    if (session.receive<Boolean>().unwrap { it }) {
        currentStep(SYNCHRONIZING)
        subFlow(SynchronizeMembershipFlowHandler(session))
    }
}

@Suspendable
fun FlowLogic<*>.resolveOurMembershipAndAttestation(
    network: Network,
    holder: AbstractParty = ourIdentity
): Pair<StateAndRef<Membership>, StateAndRef<MembershipAttestation>> {
    val ourMembership = subFlow(FindMembershipFlow(holder = holder, network = network))
        ?: throw FlowException("Failed to find membership for '$holder' on '$network'.")

    val ourAttestation = subFlow(FindMembershipAttestationFlow(holder = holder, network = network))
        ?: throw FlowException("Failed to find membership attestation for '$holder' on '$network'.")

    return ourMembership to ourAttestation
}

@Suspendable
fun FlowLogic<*>.resolveTheirMembershipAndAttestation(
    network: Network,
    session: FlowSession
): Pair<StateAndRef<Membership>, StateAndRef<MembershipAttestation>> {
    val ourMembership = subFlow(FindMembershipFlow(holder = ourIdentity, network = network))
        ?: throw FlowException("Failed to find our membership for specified network: $network.")

    val (membership, attestations) = subFlow(SynchronizeMembershipFlow(ourMembership, session))
        ?: throw FlowException("Failed to synchronize our membership with the specified holder: ${session.counterparty}")

    return membership to attestations.single()
}

@Suspendable
fun FlowLogic<*>.getTokenObservers(network: Network, holder: AbstractParty): Set<Party> {
    val membership = subFlow(FindMembershipFlow(holder = holder, network = network))
        ?: throw FlowException("Failed to find membership for '$holder' on network '${network.value}'.")

    return membership.state.data.getSettings<String>("TOKEN_OBSERVER").map {
        val name = CordaX500Name.parse(it.value)
        serviceHub.identityService.wellKnownPartyFromX500Name(name)
            ?: throw IllegalStateException("Failed to resolve '$name' to a well known party.")
    }.toSet()
}
