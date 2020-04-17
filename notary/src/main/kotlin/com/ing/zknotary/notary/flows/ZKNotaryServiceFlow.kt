package com.ing.zknotary.notary.flows

import com.ing.zknotary.common.transactions.ZKFilteredTransaction
import com.ing.zknotary.common.zkp.DefaultZKConfig
import com.ing.zknotary.common.zkp.ZKConfig
import net.corda.core.KeepForDJVM
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowSession
import net.corda.core.flows.NotarisationPayload
import net.corda.core.flows.NotaryError
import net.corda.core.identity.Party
import net.corda.core.internal.notary.NotaryInternalException
import net.corda.core.internal.notary.NotaryServiceFlow
import net.corda.core.internal.notary.SinglePartyNotaryService
import net.corda.core.node.NetworkParameters
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.ContractUpgradeFilteredTransaction
import net.corda.core.transactions.NotaryChangeWireTransaction
import java.time.Duration

// TODO: find out how to inject the ZKConfig
class ZKNotaryServiceFlow(
    otherSideSession: FlowSession,
    service: SinglePartyNotaryService,
    etaThreshold: Duration,
    private val zkConfig: ZKConfig = DefaultZKConfig
) :
    NotaryServiceFlow(otherSideSession, service, etaThreshold) {
    init {
        if (service.services.networkParameters.minimumPlatformVersion < 5) {
            throw IllegalStateException("The ZKNotaryService is compatible with Corda version 5 or greater")
        }
    }

    override fun extractParts(requestPayload: NotarisationPayload): TransactionParts {
        val tx = requestPayload.coreTransaction
        return when (tx) {
            is ZKFilteredTransaction -> TransactionParts(
                tx.id,
                tx.inputs,
                tx.timeWindow,
                tx.notary,
                tx.references,
                networkParametersHash = tx.networkParametersHash
            )
            is ContractUpgradeFilteredTransaction,
            is NotaryChangeWireTransaction -> TransactionParts(
                tx.id,
                tx.inputs,
                null,
                tx.notary,
                networkParametersHash = tx.networkParametersHash
            )
            else -> throw UnexpectedTransactionTypeException(tx)
        }
    }

    override fun verifyTransaction(requestPayload: NotarisationPayload) {
        val tx = requestPayload.coreTransaction
        try {
            when (tx) {
                is ZKFilteredTransaction -> {
                    tx.verify()
                    // TODO: the instance should be the additional Merkle root
                    val instance = zkConfig.serializer.serializeInstance(tx.id)
                    zkConfig.verifier.verify(tx.proof, instance)

                    val notary = tx.notary
                        ?: throw IllegalArgumentException("Transaction does not specify a notary.")
                    checkNotaryWhitelisted(notary, tx.networkParametersHash)
                }
                is ContractUpgradeFilteredTransaction -> {
                    checkNotaryWhitelisted(tx.notary, tx.networkParametersHash)
                }
                is NotaryChangeWireTransaction -> {
                    checkNotaryWhitelisted(tx.newNotary, tx.networkParametersHash)
                }
                else -> throw UnexpectedTransactionTypeException(tx)
            }
        } catch (e: Exception) {
            throw NotaryInternalException(NotaryError.TransactionInvalid(e))
        }
    }

    /** Make sure the transaction notary is part of the network parameter whitelist. */
    private fun checkNotaryWhitelisted(notary: Party, attachedParameterHash: SecureHash?) {
        // Expecting network parameters to be attached for platform version 4 or later.
        if (attachedParameterHash == null) {
            throw IllegalArgumentException("Transaction must contain network parameters.")
        }
        val attachedParameters = serviceHub.networkParametersService.lookup(attachedParameterHash)
            ?: throw IllegalStateException("Unable to resolve network parameters from hash: $attachedParameterHash")

        checkInWhitelist(attachedParameters, notary)
    }

    private fun checkInWhitelist(networkParameters: NetworkParameters, notary: Party) {
        val notaryWhitelist = networkParameters.notaries.map { it.identity }

        check(notary in notaryWhitelist) {
            "Notary specified by the transaction ($notary) is not on the network parameter whitelist: ${notaryWhitelist.joinToString()}"
        }
    }

    @KeepForDJVM
    @CordaSerializable
    class UnexpectedTransactionTypeException(tx: Any) : IllegalArgumentException(
        "Received unexpected transaction type: " +
            "${tx::class.java.simpleName}, expected ${ZKFilteredTransaction::class.java.simpleName}, " +
            "${ContractUpgradeFilteredTransaction::class.java.simpleName} or ${NotaryChangeWireTransaction::class.java.simpleName}"
    )
}
