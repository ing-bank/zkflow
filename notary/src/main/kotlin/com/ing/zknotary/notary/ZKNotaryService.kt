package com.ing.zknotary.notary

import com.ing.zknotary.common.flows.getCordaServiceFromConfig
import com.ing.zknotary.common.zkp.ZKConfig
import com.ing.zknotary.notary.flows.ZKNotaryServiceFlow
import java.security.PublicKey
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.internal.notary.SinglePartyNotaryService
import net.corda.core.schemas.MappedSchema
import net.corda.core.utilities.seconds
import net.corda.node.services.api.ServiceHubInternal
import net.corda.node.services.transactions.NodeNotarySchema
import net.corda.node.services.transactions.PersistentUniquenessProvider

class ZKNotaryService(override val services: ServiceHubInternal, override val notaryIdentityKey: PublicKey) :
    SinglePartyNotaryService() {
    override val uniquenessProvider =
        PersistentUniquenessProvider(services.clock, services.database, services.cacheFactory, ::signTransaction)

    /**
     * By loading this on construction, we enforce valid config.
     * This prevents runtime exceptions on bad config.
     */
    private val zkConfig = ZKConfig(
        prover = services.getCordaServiceFromConfig("zkpProver"),
        verifier = services.getCordaServiceFromConfig("zkpVerifier"),
        serializationFactoryService = services.getCordaServiceFromConfig("zkpSerializationFactoryService")
    )

    init {
        if (services.networkParameters.minimumPlatformVersion < 5) {
            throw IllegalStateException("The ZKNotaryService is compatible with Corda version 5 or greater")
        }
    }

    override fun createServiceFlow(otherPartySession: FlowSession): FlowLogic<Void?> {
        return ZKNotaryServiceFlow(
            otherPartySession,
            this,
            5.seconds, // in the real world, this should come from configuration
            zkConfig
        )
    }

    override fun start() {}
    override fun stop() {}
}

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
