package com.ing.zknotary.common.client.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.internal.concurrent.transpose
import net.corda.core.messaging.startFlow
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.unwrap
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.internal.cordappForClasses
import net.corda.testing.node.internal.enclosedCordapp
import org.junit.Test
import java.security.PublicKey
import kotlin.test.assertTrue

class CustomSerializationSchemeDriverTest {

    companion object {
        private fun createWireTx(
            serviceHub: ServiceHub,
            notary: Party,
            key: PublicKey,
            schemeId: Int
        ): WireTransaction {
            val outputState = TransactionState(
                data = DummyContract.DummyState(),
                contract = DummyContract::class.java.name,
                notary = notary,
                constraint = AlwaysAcceptAttachmentConstraint
            )
            val builder = TransactionBuilder()
                .addOutputState(outputState)
                .addCommand(DummyCommandData, key)
            return builder.toWireTransaction(serviceHub, schemeId)
        }
    }

    @Test(timeout = 300_000)
    fun `flow can send wire transaction serialized with custom serialization scheme `() {
        driver(
            DriverParameters(
                notarySpecs = emptyList(),
                startNodesInProcess = true,
                cordappsForAllNodes = listOf(enclosedCordapp(), cordappForClasses(TestSerializationScheme::class.java))
            )
        ) {
            val (alice, bob) = listOf(
                startNode(NodeParameters(providedName = ALICE_NAME)),
                startNode(NodeParameters(providedName = BOB_NAME))
            ).transpose().getOrThrow()

            val flow = alice.rpc.startFlow(::SendFlow, bob.nodeInfo.legalIdentities.single())
            assertTrue { flow.returnValue.getOrThrow() }
        }
    }

    @StartableByRPC
    @InitiatingFlow
    class SendFlow(val counterparty: Party) : FlowLogic<Boolean>() {
        @Suspendable
        override fun call(): Boolean {
            val wtx = createWireTx(serviceHub, counterparty, counterparty.owningKey, TestSerializationScheme.SCHEME_ID)
            val session = initiateFlow(counterparty)
            session.send(wtx)
            return session.receive<Boolean>().unwrap { it }
        }
    }

    @InitiatedBy(SendFlow::class)
    class ReceiveFlow(private val session: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val message = session.receive<WireTransaction>().unwrap { it }
            message.toLedgerTransaction(serviceHub)
            session.send(true)
        }
    }

    class DummyContract : Contract {
        @BelongsToContract(DummyContract::class)
        class DummyState(override val participants: List<AbstractParty> = listOf()) : ContractState

        override fun verify(tx: LedgerTransaction) {
            return
        }
    }

    object DummyCommandData : TypeOnlyCommandData()
}
