package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.zkp.MockZKTransactionService
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.common.zkp.ZincZKTransactionService
import com.ing.zknotary.node.services.collectVerifiedDependencies
import com.ing.zknotary.node.services.toZKVerifierTransaction
import com.ing.zknotary.nodes.services.MockZKTransactionStorage
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import net.corda.testing.node.MockServices
import net.corda.testing.node.createMockCordaService
import net.corda.testing.node.ledger
import java.io.File
import java.nio.ByteBuffer
import java.time.Duration
import kotlin.streams.toList
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class ProverService {
    val ledgerServices: MockServices
    val zkTransactionServices: Map<SecureHash, ZKTransactionService>
    val zkStorage: MockZKTransactionStorage

    constructor(ledgerServices: MockServices, circuits: Map<SecureHash, String>) {
        this.ledgerServices = ledgerServices
        zkStorage = createMockCordaService(ledgerServices, ::MockZKTransactionStorage)

        println("\nSetting up ${circuits.size} circuit(s), this may take some minutes")
        val overallSetupDuration = measureTime {
            zkTransactionServices = circuits.entries.parallelStream().map { (circuitId, circuitPath) ->
                println("Starting for $circuitPath")

                val circuitFolder = File(circuitPath).absolutePath
                val artifactFolder = File("$circuitFolder/artifacts")
                artifactFolder.mkdirs()

                val zkTransactionService = ZincZKTransactionService(
                    circuitFolder,
                    artifactFolder = artifactFolder.absolutePath,
                    buildTimeout = Duration.ofSeconds(10 * 60),
                    setupTimeout = Duration.ofSeconds(10 * 60),
                    provingTimeout = Duration.ofSeconds(10 * 60),
                    verificationTimeout = Duration.ofSeconds(10 * 60)
                )

                val setupDuration = measureTime {
                    zkTransactionService.setup()
                }

                println("Setup duration for $circuitPath: ${setupDuration.inMinutes} mins")

                circuitId to zkTransactionService
            }
                .toList()
                .toMap()
            // Impossible to immediately collect into a Map.
        }

        println("Overall setup duration: ${overallSetupDuration.inMinutes} mins")
    }

    constructor(ledgerServices: MockServices) {
        println("Mocking up ProverService")

        this.ledgerServices = ledgerServices
        zkTransactionServices = mapOf(
            SecureHash.allOnesHash as SecureHash to createMockCordaService(
                ledgerServices,
                ::MockZKTransactionService
            )
        )
        zkStorage = createMockCordaService(ledgerServices, ::MockZKTransactionStorage)
    }

    /**
     Proves a single tx.
     */
    private fun prove(txId: SecureHash) {
        println("\nProving tx: ${txId.toString().take(8)}")
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
                zkStorage,
                zkTransactionService,
                persist = true
            )

            vtxId = vtx.id
        }

        println("\t${txId.toString().take(8)} => ${vtxId?.toString()?.take(8)} in $provingTime")
    }

    /**
     Given a wire tx, proves it and all txs leading to it.
     */
    fun prove(tx: WireTransaction) {
        println("Proving chain leading to: ${tx.id.toString().take(8)}")
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
        println("Overall proving time: ${provingTime.inMinutes} mins")
    }
}
