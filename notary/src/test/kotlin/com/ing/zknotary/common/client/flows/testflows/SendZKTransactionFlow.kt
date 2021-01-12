package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.common.transactions.SignedZKVerifierTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.utilities.unwrap

/**
 * Dummy test code, not intended to be used in production
 */
class SendZKTransactionFlow(val session: FlowSession, val tx: SignedZKVerifierTransaction) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        val txStorage = serviceHub.getCordaServiceFromConfig<ZKVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        // Send tx itself
        session.send(tx)

        // Send backchain transactions
        tx.tx.inputs.forEach {
            session.send(
                txStorage.getTransaction(it.txhash)!!
            )
        }
    }
}

/**
 * Dummy test code, not intended to be used in production
 */
class ReceiveZKTransactionFlow(val session: FlowSession) : FlowLogic<SignedZKVerifierTransaction>() {

    @Suspendable
    override fun call(): SignedZKVerifierTransaction {

        val zkTransactionService =
            serviceHub.getCordaServiceFromConfig<ZKTransactionService>(ServiceNames.ZK_TX_SERVICE)

        val zkStorage =
            serviceHub.getCordaServiceFromConfig<ZKWritableVerifierTransactionStorage>(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        val tx = session.receive<SignedZKVerifierTransaction>().unwrap { it }

        // verifying backchain now
        tx.tx.inputs.forEach {
            val zktx = session.receive<SignedZKVerifierTransaction>().unwrap { it }
            zkTransactionService.verify(zktx) // Clearly only works with backchain of length 1 for now
            zkStorage.addTransaction(zktx)
        }

        return tx
    }
}
