package com.ing.zknotary.notary

import com.ing.zknotary.common.flows.getCordaServiceFromConfig
import com.ing.zknotary.common.serializer.ZKInputSerializer
import com.ing.zknotary.common.zkp.ZKConfig
import com.ing.zknotary.notary.flows.ZKNotaryServiceFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.internal.notary.SinglePartyNotaryService
import net.corda.core.schemas.MappedSchema
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.utilities.seconds
import net.corda.node.services.api.ServiceHubInternal
import net.corda.node.services.transactions.NodeNotarySchema
import net.corda.node.services.transactions.PersistentUniquenessProvider
import java.security.PublicKey

class ZKNotaryService(override val services: ServiceHubInternal, override val notaryIdentityKey: PublicKey) :
    SinglePartyNotaryService() {
    override val uniquenessProvider =
        PersistentUniquenessProvider(services.clock, services.database, services.cacheFactory, ::signTransaction)

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
            ZKConfig(
                prover = services.getCordaServiceFromConfig("zkpProver"),
                verifier = services.getCordaServiceFromConfig("zkpVerifier"),
                serializer = services.getCordaServiceFromConfig("zkpSerializer")
            )

        )
    }

    // /**
    //  * Returns a singleton of type [T].
    //  *
    //  * This function throws an exception if:
    //  * - the config key does not exist in the CorDapp
    //  * - the config value refers to a class that doesn't exist. (Note that it should be a fully qualified name).
    //  * - the class is not a properly registered [CordaService]
    //  */
    // private fun <T : SerializeAsToken> getCordaServiceFromConfig(configKey: String): T {
    //     val config = services.getAppContext().config
    //
    //     @Suppress("UNCHECKED_CAST")
    //     val serializerClass = Class.forName(config.getString("zkp.verifier")) as Class<T>
    //
    //     return services.cordaService(serializerClass)
    // }

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
