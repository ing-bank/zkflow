/*
 * Source attribution:
 *
 * Some flows in this file are strongly based on their original non-ZKP counterpart (i.e. without the 'ZK' prefix in the class name) from Corda
 * itself, as defined in the package net.corda.core.flows (https://github.com/corda/corda).
 *
 * Ideally ZKFlow could have extended the Corda flows to add the ZKP checks only, and leave the rest of the behaviour intact.
 * Unfortunately, Corda's flows were not implemented with extension in mind, and it was not possible to create this flow without copying most
 * of the original flow.
 */
package com.ing.zkflow.notary

import co.paralleluniverse.fibers.Suspendable
import com.ing.zkflow.common.node.services.ServiceNames
import com.ing.zkflow.common.node.services.getCordaServiceFromConfig
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.notary.flows.ZKNotaryServiceFlow
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowExternalAsyncOperation
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.NotarisationRequestSignature
import net.corda.core.identity.Party
import net.corda.core.internal.notary.NotaryInternalException
import net.corda.core.internal.notary.NotaryService
import net.corda.core.internal.notary.UniquenessProvider
import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.seconds
import net.corda.node.services.api.ServiceHubInternal
import net.corda.node.services.transactions.PersistentUniquenessProvider
import java.security.PublicKey
import java.time.Duration
import java.util.concurrent.CompletableFuture

open class ZKNotaryService(final override val services: ServiceHubInternal, override val notaryIdentityKey: PublicKey) :
    NotaryService() {

    val uniquenessProvider = PersistentUniquenessProvider(
        services.clock,
        services.database,
        services.cacheFactory,
        ::signTransaction
    )

    /**
     * Estimate the wait time to be notarised taking into account the new request size.
     *
     * @param numStates The number of states we're about to request be notarised.
     */
    fun getEstimatedWaitTime(numStates: Int): Duration = uniquenessProvider.getEta(numStates)

    fun signTransaction(txId: SecureHash): TransactionSignature {

        val signatureMetadata = SignatureMetadata(services.myInfo.platformVersion, Crypto.findSignatureScheme(notaryIdentityKey).schemeNumberID)
        val signableData = SignableData(txId, signatureMetadata)

        return TransactionSignature(
            services.keyManagementService.sign(signableData, notaryIdentityKey).bytes,
            notaryIdentityKey,
            signatureMetadata
        )
    }

    val notaryConfig = services.configuration.notary
        ?: throw IllegalArgumentException("Failed to register ${this::class.java}: notary configuration not present")

    /**
     * By loading this on construction, we enforce valid config.
     * This prevents runtime exceptions on bad config.
     */
    private val zkConfig = NotaryZKConfig(
        zkTransactionService = services.getCordaServiceFromConfig(ServiceNames.ZK_TX_SERVICE),
        zkVerifierTransactionStorage = services.getCordaServiceFromConfig(ServiceNames.ZK_VERIFIER_TX_STORAGE)
    )

    init {
        if (services.networkParameters.minimumPlatformVersion < ZKFlow.REQUIRED_PLATFORM_VERSION) {
            throw IllegalStateException("The ZKNotaryService is compatible with Corda version ${ZKFlow.REQUIRED_PLATFORM_VERSION} or greater")
        }
    }

    override fun createServiceFlow(otherPartySession: FlowSession): FlowLogic<Void?> {
        return ZKNotaryServiceFlow(
            otherPartySession,
            this,
            notaryConfig.etaMessageThresholdSeconds.seconds,
            zkConfig
        )
    }

    /**
     * Required for the flow to be able to suspend until the commit is complete.
     * This object will be included in the flow checkpoint.
     */
    @Suppress("LongParameterList")
    @CordaSerializable
    class CommitOperation(
        val service: ZKNotaryService,
        val inputs: List<StateRef>,
        val txId: SecureHash,
        val caller: Party,
        val requestSignature: NotarisationRequestSignature,
        val timeWindow: TimeWindow?,
        val references: List<StateRef>
    ) : FlowExternalAsyncOperation<UniquenessProvider.Result> {

        override fun execute(deduplicationId: String): CompletableFuture<UniquenessProvider.Result> {
            return service.uniquenessProvider.commit(inputs, txId, caller, requestSignature, timeWindow, references).toCompletableFuture()
        }
    }

    /** Attempts to commit the specified transaction [txId]. */
    @Suppress("LongParameterList")
    @Suspendable
    open fun commitStates(
        inputs: List<StateRef>,
        txId: SecureHash,
        caller: Party,
        requestSignature: NotarisationRequestSignature,
        timeWindow: TimeWindow?,
        references: List<StateRef>
    ): UniquenessProvider.Result {
        val callingFlow = FlowLogic.currentTopLevel
            ?: throw IllegalStateException("This method should be invoked in a flow context.")

        val result = callingFlow.await(
            CommitOperation(
                this,
                inputs,
                txId,
                caller,
                requestSignature,
                timeWindow,
                references
            )
        )

        if (result is UniquenessProvider.Result.Failure) {
            throw NotaryInternalException(result.error)
        }

        return result
    }

    override fun start() {}
    override fun stop() {}
}

object NodeNotarySchema

object PersistentUniquenessProviderSchema : MappedSchema(
    schemaFamily = NodeNotarySchema.javaClass, version = 1,
    mappedTypes = listOf(
        PersistentUniquenessProvider.BaseComittedState::class.java,
        PersistentUniquenessProvider.Request::class.java,
        PersistentUniquenessProvider.CommittedState::class.java,
        PersistentUniquenessProvider.CommittedTransaction::class.java
    )
) {
    override val migrationResource = "node-notary.changelog-master"
}
