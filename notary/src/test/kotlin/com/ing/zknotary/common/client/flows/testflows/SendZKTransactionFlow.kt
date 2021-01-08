package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

/**
 * Dummy test code, not intended to be used in production
 */
class SendZKTransactionFlow(val session: FlowSession, val tx: SignedTransaction) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        val txStorage = serviceHub.getCordaServiceFromConfig<ZKVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        // Send tx itself
        session.send(tx)

        // Send backchain transactions
        tx.inputs.forEach {
            session.send(
                txStorage.getTransaction(
                    txStorage.map.get(it.txhash)!!
                )!!
            )
        }
    }
}

/**
 * Dummy test code, not intended to be used in production
 */
class ReceiveZKTransactionFlow(val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val zkTransactionService =
            serviceHub.getCordaServiceFromConfig<ZKTransactionService>(ServiceNames.ZK_TX_SERVICE)

        val tx = session.receive<SignedTransaction>().unwrap { it }

        // verifying backchain now
        tx.inputs.forEach {
            val zktx = session.receive<SignedZKVerifierTransaction>().unwrap { it }
            zkTransactionService.verify(zktx) // Clearly only works with backchain of length 1 for now
            // Also we don't store transaction here
        }

        return tx
    }
}
