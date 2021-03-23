package com.ing.zknotary.common.client.flows.testflows

import co.paralleluniverse.fibers.Suspendable
import com.ing.zknotary.client.flows.ZKSignTransactionFlow
import com.ing.zknotary.common.flows.SendUtxoInfosFlow
import com.ing.zknotary.common.flows.ZKReceiveStateAndRefFlow
import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import com.ing.zknotary.common.transactions.collectSerializedUtxosAndNonces
import com.ing.zknotary.common.transactions.signInitialTransaction
import com.ing.zknotary.common.transactions.toLedgerTransaction
import com.ing.zknotary.common.zkp.UtxoInfo
import com.ing.zknotary.node.services.ServiceNames
import com.ing.zknotary.node.services.ZKVerifierTransactionStorage
import com.ing.zknotary.node.services.getCordaServiceFromConfig
import com.ing.zknotary.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.ContractUpgradeWireTransaction
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.NotaryChangeWireTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap


/**
 * Disclaimer: this is not how it is supposed to be used in "real" flows, it works just for this test
 */
@InitiatingFlow
class MoveBidirectionalFlow(
    private val myInput: StateAndRef<TestContract.TestState>,
    private val counterParty: Party
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val session = initiateFlow(counterParty)

        // Initiator sends proposed state to exchange.
        session.send(myInput)

        // Expects UtxoInfo for a state of the same value in return.
        val theirInput = subFlow<List<StateAndRef<TestContract.TestState>>>(ZKReceiveStateAndRefFlow(session)).single()

        // Now we create the transaction
        val me = serviceHub.myInfo.legalIdentities.single()
        val command = Command(TestContract.MoveBidirectional(), listOf(counterParty, me).map { it.owningKey })
        val myOutput = StateAndContract(myInput.state.data.copy(owner = counterParty), TestContract.PROGRAM_ID)
        val theirOutput = StateAndContract(theirInput.state.data.copy(owner = me), TestContract.PROGRAM_ID)

        val builder = ZKTransactionBuilder(TransactionBuilder(serviceHub.networkMapCache.notaryIdentities.single()))
        builder.withItems(myInput, theirInput, myOutput, theirOutput, command)

        // Transaction creator signs transaction.
        val stx = serviceHub.signInitialTransaction(builder)

        // TODO: this fails for now, because we don't have the tx history for theirInput.
        // Will this work when we have executed the DataVendingFlow first? Or still not?
        // more logically, we will do vtx.verify(), where we get the hashes for the inputs/referecens from
        // the resolved zkvtransactions
        stx.zkVerify(serviceHub, false, listOf(theirInput))

//        val vtx = zkService.prove(stx.tx, listOf(theirUtxoInfo))
//
//        val partiallySignedVtx = signInitialZKTransaction(vtx)
//        val svtx = subFlow(ZKCollectSignaturesFlow(stx, partiallySignedVtx, listOf(session)))
//
//        subFlow(ZKFinalityFlow(stx, svtx, listOf(session)))

        return stx
    }

    fun SignedTransaction.zkVerify(
        services: ServiceHub,
        checkSufficientSignatures: Boolean = true,
        receivedInputs: List<StateAndRef<ContractState>> = emptyList(),
        receivedReferences: List<StateAndRef<ContractState>> = emptyList(),
    ) {
        zkResolveAndCheckNetworkParameters(services)
        when (coreTransaction) {
            is NotaryChangeWireTransaction -> verify(services, checkSufficientSignatures)
            is ContractUpgradeWireTransaction -> verify(services, checkSufficientSignatures)
            else -> zkVerifyRegularTransaction(services, checkSufficientSignatures, receivedInputs, receivedReferences)
        }
    }

    private fun SignedTransaction.zkVerifyRegularTransaction(
        services: ServiceHub,
        checkSufficientSignatures: Boolean,
        receivedInputs: List<StateAndRef<ContractState>>,
        receivedReferences: List<StateAndRef<ContractState>>
    ) {
        val ltx = zkToLedgerTransaction(services, checkSufficientSignatures, receivedInputs, receivedReferences)
        // This fails with a weird db access error, so we use ltx.verify
        // services.transactionVerifierService.verify(ltx).getOrThrow()
        ltx.verify()
    }

    fun SignedTransaction.zkToLedgerTransaction(
        services: ServiceHub,
        checkSufficientSignatures: Boolean = true,
        receivedInputs: List<StateAndRef<ContractState>> = emptyList(),
        receivedReferences: List<StateAndRef<ContractState>> = emptyList()
    ): LedgerTransaction {
        if (checkSufficientSignatures) {
            verifyRequiredSignatures() // It internally invokes checkSignaturesAreValid().
        } else {
            checkSignaturesAreValid()
        }
        // We need parameters check here, because finality flow calls stx.toLedgerTransaction() and then verify.
        zkResolveAndCheckNetworkParameters(services)
        return tx.toLedgerTransaction(receivedInputs, receivedReferences, services)
    }

    private fun SignedTransaction.zkResolveAndCheckNetworkParameters(services: ServiceHub) {
        val zkTxStorage: ZKVerifierTransactionStorage =
            serviceHub.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)

        val hashOrDefault = networkParametersHash ?: services.networkParametersService.defaultHash
        val txNetworkParameters = services.networkParametersService.lookup(hashOrDefault)
            ?: throw TransactionResolutionException(id)
        val groupedInputsAndRefs = (inputs + references).groupBy { it.txhash }
        groupedInputsAndRefs.map { entry ->
            val tx = zkTxStorage.getTransaction(entry.key)
                ?: throw TransactionResolutionException(id)
            val paramHash = tx.tx.networkParametersHash ?: services.networkParametersService.defaultHash
            val params = services.networkParametersService.lookup(paramHash) ?: throw TransactionResolutionException(id)
            if (txNetworkParameters.epoch < params.epoch)
                throw TransactionVerificationException.TransactionNetworkParameterOrderingException(
                    id,
                    entry.value.first(),
                    txNetworkParameters,
                    params
                )
        }
    }

    companion object {

        @InitiatedBy(MoveBidirectionalFlow::class)
        class Verifier(val session: FlowSession) : FlowLogic<Unit>() {
            @Suspendable
            override fun call() {
                val signFlow = object : ZKSignTransactionFlow(session) {
                    @Suspendable
                    override fun checkTransaction(stx: SignedTransaction) = requireThat {
                        // In non-test scenario here counterparty can verify incoming Tx from business perspective
                    }
                }
                val initiatorState = session.receive<StateAndRef<TestContract.TestState>>().unwrap { it }

                // Create a state of the same value as the one proposed by initiator
                val createStx = subFlow(CreateFlow(initiatorState.state.data.value))
                // Send it back.

                val output = createStx.tx.outRef<TestContract.TestState>(0)
                val (serialized, nonces) = serviceHub.collectSerializedUtxosAndNonces(listOf(output.ref))
                val utxoInfo = UtxoInfo(
                    stateRef = output.ref,
                    serializedContents = serialized.single(),
                    nonce = nonces.single(),
                    digestAlgorithm = createStx.tx.digestService.hashAlgorithm
                )
                subFlow(SendUtxoInfosFlow(session, listOf(utxoInfo)))

                // Invoke the signing subFlow, in response to the counterparty calling [ZKCollectSignaturesFlow].
//                val stx = subFlow(signFlow)

                // Invoke flow in response to ZKFinalityFlow
//                subFlow(ZKReceiveFinalityFlow(session, stx))
            }
        }
    }
}
