package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.collectVerifiedDependencies
import com.ing.zknotary.node.services.toZKVerifierTransaction
import com.ing.zknotary.nodes.services.MockZKProverTransactionStorage
import com.ing.zknotary.nodes.services.MockZKVerifierTransactionStorage
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import net.corda.testing.node.MockServices
import net.corda.testing.node.createMockCordaService
import net.corda.testing.node.ledger
import java.nio.ByteBuffer
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class ProverService {
    val ledgerServices: MockServices
    val zkTransactionServices: Map<SecureHash, ZKTransactionService>
    val zkVerifierTransactionStorage: MockZKVerifierTransactionStorage
    private val zkProverTransactionStorage: MockZKProverTransactionStorage

    private val logger = loggerFor<ProverService>()

    constructor(ledgerServices: MockServices, zkTransactionServices: Map<SecureHash, ZKTransactionService>) {
        logger.info("Setting up Prover Service")

        this.ledgerServices = ledgerServices
        zkVerifierTransactionStorage = createMockCordaService(ledgerServices, ::MockZKVerifierTransactionStorage)
        zkProverTransactionStorage = createMockCordaService(ledgerServices, ::MockZKProverTransactionStorage)
        this.zkTransactionServices = zkTransactionServices
    }

    /**
     Proves a single tx.
     */
    private fun prove(txId: SecureHash) {
        logger.info("Proving tx: ${txId.toString().take(8)}")
        var vtxId: SecureHash? = null
        val provingTime = measureTime {
            val stx = ledgerServices.validatedTransactions.getTransaction(txId) ?: error("Tx with id $txId not found")

            // TODO
            // Temporary calculation of the circuit id.
            val commandId = ((stx.coreTransaction as WireTransaction).commands.single().value as ZKCommandData).id
            val circuitId = SecureHash.Companion.sha256(ByteBuffer.allocate(4).putInt(commandId).array())

            val zkTransactionService = zkTransactionServices[circuitId]
                ?: zkTransactionServices[SecureHash.allOnesHash]
                ?: error("Unknown circuit for command id $commandId")

            val vtx = stx.toZKVerifierTransaction(
                ledgerServices,
                zkProverTransactionStorage,
                zkVerifierTransactionStorage,
                zkTransactionService,
                persist = true
            )

            vtxId = vtx.id
        }

        logger.debug("\t${txId.toString().take(8)} => ${vtxId?.toString()?.take(8)} in $provingTime")
    }

    /**
     Given a wire tx, proves it and all txs leading to it.
     */
    fun prove(tx: WireTransaction) {
        logger.info("Proving chain leading to: ${tx.id.toString().take(8)}")
        val provingTime = measureTime {
            ledgerServices.ledger {
                // First, if not done before,  the prover makes sure it has all SignedTransactions by calling ResolveTransactionsFlow
                // Then, the prover walks through the list of stxs in order from issuance, leading to the stx to prove,
                // and creates ptxs out of them. This requires changing txhashes in the StateRefs to the just calculated
                // txhashes of the newly created ptxs.
                val orderedDeps =
                    ledgerServices.validatedTransactions.collectVerifiedDependencies(tx.inputs + tx.references)

                // Create and store all vtxs ordered from issuances up to head tx
                (orderedDeps + tx.id).forEach {
                    prove(it)
                }
            }
        }
        logger.debug("Overall proving time: ${provingTime.inMinutes} mins")
    }
}
