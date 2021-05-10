package io.ivno.collateraltoken.integration

import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.workflow.deposit.*
import io.onixlabs.corda.core.integration.RPCService
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowProgressHandle
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import java.util.*

class DepositCommandService(rpc: CordaRPCOps) : RPCService(rpc) {

    private companion object {
        val logger = loggerFor<DepositCommandService>()
    }

    fun requestDeposit(
        depositor: Party,
        custodian: Party,
        tokenIssuingEntity: Party,
        amount: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        accountId: String,
        linearId: UniqueIdentifier = UniqueIdentifier(),
        notary: Party? = null,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val deposit = Deposit(depositor, custodian, tokenIssuingEntity, amount, accountId, linearId)
        logger.info("Requesting deposit: $deposit.")
        return rpc.startTrackedFlow(RequestDepositFlow::Initiator, deposit, notary, observers)
    }

    fun acceptDeposit(
        deposit: StateAndRef<Deposit>,
        reference: String = UUID.randomUUID().toString(),
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val acceptedDeposit = deposit.state.data.acceptDeposit(reference)
        logger.info("Accepting deposit: $acceptedDeposit.")
        return rpc.startTrackedFlow(AcceptDepositFlow::Initiator, deposit, acceptedDeposit, observers)
    }

    fun rejectDeposit(
        deposit: StateAndRef<Deposit>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val rejectedDeposit = deposit.state.data.rejectDeposit()
        logger.info("Rejecting deposit: $rejectedDeposit.")
        return rpc.startTrackedFlow(RejectDepositFlow::Initiator, deposit, rejectedDeposit, observers)
    }

    fun cancelDeposit(
        deposit: StateAndRef<Deposit>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val cancelledDeposit = deposit.state.data.cancelDeposit()
        logger.info("Cancelling deposit: $cancelledDeposit.")
        return rpc.startTrackedFlow(CancelDepositFlow::Initiator, deposit, cancelledDeposit, observers)
    }

    fun issuePayment(
        deposit: StateAndRef<Deposit>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val paymentIssuedDeposit = deposit.state.data.issuePayment()
        logger.info("Issuing deposit payment: $paymentIssuedDeposit.")
        return rpc.startTrackedFlow(IssueDepositPaymentFlow::Initiator, deposit, paymentIssuedDeposit, observers)
    }

    fun acceptPayment(
        deposit: StateAndRef<Deposit>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val paymentAcceptedDeposit = deposit.state.data.acceptPayment()
        logger.info("Accepting deposit payment: $paymentAcceptedDeposit.")
        return rpc.startTrackedFlow(AcceptDepositPaymentFlow::Initiator, deposit, paymentAcceptedDeposit, observers)
    }

    fun rejectPayment(
        deposit: StateAndRef<Deposit>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val paymentRejectedDeposit = deposit.state.data.rejectPayment()
        logger.info("Rejecting deposit payment: $paymentRejectedDeposit.")
        return rpc.startTrackedFlow(RejectDepositPaymentFlow::Initiator, deposit, paymentRejectedDeposit, observers)
    }
}
