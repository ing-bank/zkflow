package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.token.ExampleToken
import com.example.contract.token.commands.RedeemPrivate
import com.ing.zkflow.client.flows.ZKCollectSignaturesFlow
import com.ing.zkflow.client.flows.ZKFinalityFlow
import com.ing.zkflow.client.flows.ZKReceiveFinalityFlow
import com.ing.zkflow.client.flows.ZKSignTransactionFlow
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.resolvePublicOrPrivateStateRef
import com.ing.zkflow.common.transactions.signInitialTransaction
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.internal.SerializedStateAndRef
import net.corda.core.transactions.SignedTransaction

/**
 * Use this flow to redeem a [ExampleToken] privately.
 * Only the issuer and the holder will be aware of the token's redemption.
 *
 * This flow should be called by the holder.
 */
@InitiatingFlow
class RedeemPrivateExampleTokenFlow(
    private val stateAndRef: StateAndRef<ExampleToken>,
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val token = stateAndRef.state.data

        val redeemCommand = Command(RedeemPrivate(), listOf(token.issuer.owningKey, token.holder.owningKey))
        val builder = ZKTransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single()).withItems(stateAndRef, redeemCommand)

        val stx = serviceHub.signInitialTransaction(builder)

        val issuerSession = initiateFlow(token.issuer)
        val fullySignedTx = subFlow(ZKCollectSignaturesFlow(stx, listOf(issuerSession)))

        subFlow(ZKFinalityFlow(fullySignedTx, privateSessions = listOf(issuerSession), publicSessions = emptyList()))

        return stx
    }
}

@InitiatedBy(RedeemPrivateExampleTokenFlow::class)
class RedeemPrivateExampleTokenFlowFlowHandler(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            val signFlow = object : ZKSignTransactionFlow(otherSession) {
                @Suspendable
                override fun checkTransaction(stx: SignedTransaction) {
                    val command = stx.tx.commands.single { it.value is RedeemPrivate }.value as RedeemPrivate
                    val redeemedIndex = command.metadata.inputs.single().index
                    val redeemedStateRef = stx.tx.inputs[redeemedIndex]
                    val redeemed = SerializedStateAndRef(
                        resolvePublicOrPrivateStateRef(
                            redeemedStateRef,
                            serviceHub,
                            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE),
                            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_UTXO_INFO_STORAGE)
                        ), redeemedStateRef
                    ).toStateAndRef().state.data as? ExampleToken ?: error("Expected a ExampleToken as input")
                    require(serviceHub.myInfo.isLegalIdentity(redeemed.issuer)) { "We did not issue this token. Issuer: ${redeemed.issuer}" }
                }
            }

            subFlow(signFlow)

            subFlow(ZKReceiveFinalityFlow(otherSession))
        }
    }
}
