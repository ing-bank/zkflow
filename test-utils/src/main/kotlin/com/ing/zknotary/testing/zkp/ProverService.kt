package com.ing.zknotary.testing.zkp

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.transactions.toSignedZKVerifierTransaction
import com.ing.zknotary.common.transactions.toZKProverTransaction
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.MockZKProverTransactionStorage
import com.ing.zknotary.node.services.MockZKVerifierTransactionStorage
import com.ing.zknotary.node.services.collectVerifiedDependencies
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import net.corda.testing.node.MockServices
import net.corda.testing.node.createMockCordaService
import net.corda.testing.node.ledger
import java.nio.ByteBuffer
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
public class ProverService public constructor(
    public val ledgerServices: MockServices,
    public val zkTransactionServices: Map<SecureHash, ZKTransactionService>
) {
    public val zkVerifierTransactionStorage: MockZKVerifierTransactionStorage
    private val zkProverTransactionStorage: MockZKProverTransactionStorage

    private val logger = loggerFor<ProverService>()

    init {
        logger.info("Setting up Prover Service")
        zkVerifierTransactionStorage = createMockCordaService(ledgerServices, ::MockZKVerifierTransactionStorage)
        zkProverTransactionStorage = createMockCordaService(ledgerServices, ::MockZKProverTransactionStorage)
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

            val ptx = stx.tx.toZKProverTransaction(
                ledgerServices,
                zkProverTransactionStorage,
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = PedersenDigestService
            )

            val zkSigs: List<TransactionSignature> = ptx.command.signers.map { signer ->

                ledgerServices.keyManagementService.sign(
                    SignableData(
                        ptx.id,
                        SignatureMetadata(ledgerServices.myInfo.platformVersion, Crypto.findSignatureScheme(signer).schemeNumberID)
                    ),
                    signer
                )
            }

            val vtx = stx.toSignedZKVerifierTransaction(
                ledgerServices,
                zkSigs,
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
    public fun prove(tx: WireTransaction) {
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
