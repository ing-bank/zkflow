@file:Suppress("TooManyFunctions")

package com.ing.zkflow.common.transactions.verification

import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.ZKVerifierTransactionStorage
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.util.FEATURE_MISSING
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.ContractUpgradeWireTransaction
import net.corda.core.transactions.NotaryChangeWireTransaction
import net.corda.core.transactions.SignedTransaction

fun SignedTransaction.zkVerify(
    services: ServiceHub,
    zkService: ZKTransactionService = services.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE),
    zkTxStorage: ZKVerifierTransactionStorage = services.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE),
    checkSufficientSignatures: Boolean = true,
) {
    zkResolveAndCheckNetworkParameters(services, zkTxStorage)
    when (coreTransaction) {
        is NotaryChangeWireTransaction -> FEATURE_MISSING("ZKFlow does not yet support NotaryChangeWireTransactions.")
        is ContractUpgradeWireTransaction -> throw IllegalStateException(
            "ContractUpgradeWireTransactions are not supported in ZKFlow. Please use one of the upgrade commands generated by ZKFlow for your state and version."
        )
        else -> zkVerifyRegularTransaction(services, zkService, checkSufficientSignatures)
    }
}

/**
 * Note that this does not actually verify a proof, because it is a SignedTransaction.
 * It will run the private ZKP contract logic and the normal contract logic on the transaction to confirm correctness.
 */
private fun SignedTransaction.zkVerifyRegularTransaction(
    services: ServiceHub,
    zkService: ZKTransactionService,
    checkSufficientSignatures: Boolean,
) {
    val zkTransactionVerifierService = ZKTransactionVerifierService(
        services,
        zkService
    )

    zkTransactionVerifierService.verify(this, checkSufficientSignatures)
}

private fun SignedTransaction.zkResolveAndCheckNetworkParameters(
    services: ServiceHub,
    zkTxStorage: ZKVerifierTransactionStorage,
) {
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

fun SignedTransaction.prove(
    services: ServiceHub,
    zkService: ZKTransactionService = services.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE)
): SignedZKVerifierTransaction {
    return SignedZKVerifierTransaction(zkService.prove(tx), sigs)
}
