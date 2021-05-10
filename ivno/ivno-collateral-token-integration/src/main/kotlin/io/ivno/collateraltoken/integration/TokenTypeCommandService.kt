package io.ivno.collateraltoken.integration

import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.workflow.token.CreateIvnoTokenTypeFlow
import io.ivno.collateraltoken.workflow.token.PublishIvnoTokenTypeFlow
import io.ivno.collateraltoken.workflow.token.UpdateIvnoTokenTypeFlow
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.core.integration.RPCService
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowProgressHandle
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor

class TokenTypeCommandService(rpc: CordaRPCOps) : RPCService(rpc) {

    private companion object {
        val logger = loggerFor<TokenTypeCommandService>()
    }

    fun createTokenType(
        network: Network,
        custodian: Party,
        tokenIssuingEntity: Party,
        displayName: String,
        fractionDigits: Int = 0,
        linearId: UniqueIdentifier = UniqueIdentifier(),
        notary: Party? = null,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val tokenType = IvnoTokenType(network, custodian, tokenIssuingEntity, displayName, fractionDigits, linearId)
        logger.info("Creating token type: $tokenType.")
        return rpc.startTrackedFlow(CreateIvnoTokenTypeFlow::Initiator, tokenType, notary, observers)
    }

    fun updateTokenType(
        oldTokenType: StateAndRef<IvnoTokenType>,
        custodian: Party = oldTokenType.state.data.custodian,
        tokenIssuingEntity: Party = oldTokenType.state.data.tokenIssuingEntity,
        displayName: String = oldTokenType.state.data.displayName,
        fractionDigits: Int = oldTokenType.state.data.fractionDigits,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        val updatedTokenType = oldTokenType.state.data.copy(
            custodian = custodian,
            tokenIssuingEntity = tokenIssuingEntity,
            displayName = displayName,
            fractionDigits = fractionDigits
        )
        logger.info("Updating token type: $updatedTokenType.")
        return rpc.startTrackedFlow(UpdateIvnoTokenTypeFlow::Initiator, oldTokenType, updatedTokenType, observers)
    }

    fun publishTokenType(
        tokenTypeId: UniqueIdentifier,
        observers: Set<Party> = emptySet()
    ): FlowProgressHandle<SignedTransaction> {
        logger.info("Publishing token type: $tokenTypeId.")
        return rpc.startTrackedFlow(PublishIvnoTokenTypeFlow::Initiator, tokenTypeId, observers)
    }
}
