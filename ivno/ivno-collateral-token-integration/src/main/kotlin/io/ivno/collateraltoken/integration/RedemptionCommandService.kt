package io.ivno.collateraltoken.integration

import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.workflow.redemption.CompleteRedemptionFlow
import io.ivno.collateraltoken.workflow.redemption.RejectRedemptionFlow
import io.ivno.collateraltoken.workflow.redemption.RequestRedemptionFlow
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

class RedemptionCommandService(rpc: CordaRPCOps) : RPCService(rpc) {

    private companion object {
        val logger = loggerFor<RedemptionCommandService>()
    }

    fun requestRedemption(
        redeemer: Party,
        custodian: Party,
        tokenIssuingEntity: Party,
        amount: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        accountId: String,
        linearId: UniqueIdentifier = UniqueIdentifier(),
        notary: Party? = null,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val redemption = Redemption(redeemer, custodian, tokenIssuingEntity, amount, accountId, linearId)
        logger.info("Requesting redemption: $redemption.")
        return rpc.startTrackedFlow(RequestRedemptionFlow::Initiator, redemption, notary, observers)
    }

    fun completeRedemption(
        redemption: StateAndRef<Redemption>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val completedRedemption = redemption.state.data.completeRedemption()
        logger.info("Completing redemption: $completedRedemption.")
        return rpc.startTrackedFlow(CompleteRedemptionFlow::Initiator, redemption, completedRedemption, observers)
    }

    fun rejectRedemption(
        redemption: StateAndRef<Redemption>,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val advancedRedemption = redemption.state.data.rejectRedemption()
        logger.info("Rejecting redemption: $advancedRedemption.")
        return rpc.startTrackedFlow(RejectRedemptionFlow::Initiator, redemption, advancedRedemption, observers)
    }
}
