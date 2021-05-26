package io.ivno.collateraltoken.services

import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.contract.TransferInitiator
import io.ivno.collateraltoken.contract.TransferStatus
import io.ivno.collateraltoken.workflow.transfer.CompleteTransferFlow
import io.onixlabs.corda.core.contract.cast
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor

/**
 * Represents a service to auto-complete accepted transfers.
 * This service tracks vault changes for transfer states that are accepted, and advances them to completed.
 *
 * @param serviceHub The service hub instance assigned to this Corda service.
 */
@CordaService
internal class AutoCompleteTransferService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    private companion object {
        val logger = loggerFor<AutoCompleteTransferService>()
        val observers: MutableList<(SignedTransaction) -> Unit> = mutableListOf()
    }

    init {
        logger.info("Transfer auto-complete service started.")
        serviceHub.vaultService.rawUpdates.subscribe {
            it.produced.forEach {
                if (it.state.data is Transfer) {
                    val transfer = it.cast<Transfer>()
                    if (transfer.state.data.isReadyForCompletion) {
                        val thread = Thread { transfer.complete() }
                        logger.info("Started transfer completion on new thread: ${thread.id}.")
                        thread.start()
                    }
                }
            }
        }
    }

    private val Transfer.isReadyForCompletion: Boolean
        get() = currentTokenHolder in serviceHub.myInfo.legalIdentities &&
                initiator == TransferInitiator.CURRENT_HOLDER &&
                status == TransferStatus.ACCEPTED

    /**
     * Allows subscription to any [SignedTransaction] instances for completed transfers.
     */
    fun subscribe(action: (SignedTransaction) -> Unit) = observers.add(action)

    @Synchronized
    private fun StateAndRef<Transfer>.complete() {
        val completedTransfer = state.data.completeTransfer()
        val flow = CompleteTransferFlow.Initiator(this, completedTransfer)
        val signedTransaction = serviceHub.startTrackedFlow(flow).returnValue.getOrThrow()
        observers.forEach { it(signedTransaction) }
    }
}
