package io.ivno.collateraltoken.integration

import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.contract.TransferInitiator
import io.ivno.collateraltoken.workflow.transfer.*
import io.onixlabs.corda.core.integration.RPCService
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowProgressHandle
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor

class TransferCommandService(rpc: CordaRPCOps) : RPCService(rpc) {

    private companion object {
        val logger = loggerFor<TransferCommandService>()
    }

    fun requestTransfer(
        currentTokenHolder: AbstractParty,
        targetTokenHolder: AbstractParty,
        initiator: TransferInitiator,
        amount: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        currentTokenHolderAccountId: String,
        targetTokenHolderAccountId: String,
        linearId: UniqueIdentifier = UniqueIdentifier(),
        notary: Party? = null,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val transfer = Transfer(
            currentTokenHolder,
            targetTokenHolder,
            initiator,
            amount,
            currentTokenHolderAccountId,
            targetTokenHolderAccountId,
            linearId
        )
        logger.info("Requesting transfer: $transfer.")
        return rpc.startTrackedFlow(RequestTransferFlow::Initiator, transfer, notary, observers)
    }

    fun acceptTransfer(
        transfer: StateAndRef<Transfer>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val acceptedTransfer = transfer.state.data.acceptTransfer()
        logger.info("Accepting transfer: $acceptedTransfer.")
        return rpc.startTrackedFlow(AcceptTransferFlow::Initiator, transfer, acceptedTransfer, observers)
    }

    fun rejectTransfer(
        transfer: StateAndRef<Transfer>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val rejectedTransfer = transfer.state.data.rejectTransfer()
        logger.info("Rejecting transfer: $rejectedTransfer.")
        return rpc.startTrackedFlow(RejectTransferFlow::Initiator, transfer, rejectedTransfer, observers)
    }

    fun completeTransfer(
        transfer: StateAndRef<Transfer>,
        observers: Set<Party> = emptySet(),
        includeTokenObservers: Boolean = true
    ): FlowProgressHandle<SignedTransaction> {
        val completedTransfer = transfer.state.data.completeTransfer()
        logger.info("Completing transfer: $completedTransfer.")
        return rpc.startTrackedFlow(
            CompleteTransferFlow::Initiator,
            transfer,
            completedTransfer,
            observers,
            includeTokenObservers
        )
    }

    fun cancelTransfer(
        transfer: StateAndRef<Transfer>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val cancelledTransfer = transfer.state.data.cancelTransfer()
        logger.info("Cancelling transfer: $cancelledTransfer.")
        return rpc.startTrackedFlow(CancelTransferFlow::Initiator, transfer, cancelledTransfer, observers)
    }
}
