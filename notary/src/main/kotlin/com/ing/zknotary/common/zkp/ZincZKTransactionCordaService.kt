package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zknotary.common.zkp.metadata.ResolvedZKTransactionMetadata
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import java.io.File

@CordaService
class ZincZKTransactionCordaService(services: AppServiceHub) : ZincZKTransactionService(services)

@CordaService
open class ZincZKTransactionService(services: ServiceHub) : AbstractZKTransactionService(services) {

    private val zkServices = mutableMapOf<ResolvedZKTransactionMetadata, ZincZKService>()

    override fun zkServiceForTransactionMetadata(metadata: ResolvedZKTransactionMetadata): ZincZKService {
        return zkServices.getOrPut(metadata) {
            val circuitFolder = metadata.buildFolder
            val artifactFolder = File(circuitFolder, "data")

            return ZincZKService(
                circuitFolder.absolutePath,
                artifactFolder.absolutePath,
                metadata.buildTimeout,
                metadata.setupTimeout,
                metadata.provingTimeout,
                metadata.verificationTimeout
            )
        }
    }

    fun setup(command: ZKTransactionMetadataCommandData, force: Boolean = false) {

        if (force) {
            cleanup(command)
        }

        val zkService = zkServiceForTransactionMetadata(command.transactionMetadata.resolved)

        val circuit = CircuitManager.CircuitDescription("${zkService.circuitFolder}/src", zkService.artifactFolder)
        CircuitManager.register(circuit)

        while (CircuitManager[circuit] == CircuitManager.Status.InProgress) {
            // An upper waiting time bound can be set up,
            // but this bound may be overly pessimistic.
            Thread.sleep(10000L)
        }

        if (CircuitManager[circuit] == CircuitManager.Status.Outdated) {
            zkService.cleanup()
            CircuitManager.inProgress(circuit)
            zkService.setup()
            CircuitManager.cache(circuit)
        }
    }

    fun cleanup(command: ZKTransactionMetadataCommandData) = zkServiceForTransactionMetadata(command.transactionMetadata.resolved).cleanup()
}
