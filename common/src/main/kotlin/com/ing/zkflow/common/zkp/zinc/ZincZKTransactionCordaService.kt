package com.ing.zkflow.common.zkp.zinc

import com.ing.zkflow.common.zkp.AbstractZKTransactionService
import com.ing.zkflow.common.zkp.CircuitManager
import com.ing.zkflow.common.zkp.ZKFlow.CIRCUITMANAGER_MAX_SETUP_WAIT_TIME_SECONDS
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.utilities.loggerFor
import java.io.File

@CordaService
class ZincZKTransactionCordaService(services: AppServiceHub) : ZincZKTransactionService(services)

@CordaService
open class ZincZKTransactionService(services: ServiceHub) : AbstractZKTransactionService(services) {
    private val zkServices = mutableMapOf<ResolvedZKCommandMetadata, ZincZKService>()
    private val log = loggerFor<ZincZKTransactionService>()

    override fun zkServiceForCommandMetadata(metadata: ResolvedZKCommandMetadata): ZincZKService {
        return zkServices.getOrPut(metadata) {
            val circuitFolder = metadata.circuit.buildFolder
            val artifactFolder = File(circuitFolder, "data")

            return ZincZKService(
                circuitFolder.absolutePath,
                artifactFolder.absolutePath,
                metadata.circuit.buildTimeout,
                metadata.circuit.setupTimeout,
                metadata.circuit.provingTimeout,
                metadata.circuit.verificationTimeout
            )
        }
    }

    override fun setup(command: ResolvedZKCommandMetadata, force: Boolean) {
        if (force) {
            cleanup(command)
        }

        val zkService = zkServiceForCommandMetadata(command)

        val circuit = CircuitManager.CircuitDescription("${zkService.circuitFolder}/src", zkService.artifactFolder)
        CircuitManager.register(circuit)

        while (CircuitManager[circuit] == CircuitManager.Status.InProgress) {
            log.debug("CircuitManager in progress. Waiting $CIRCUITMANAGER_MAX_SETUP_WAIT_TIME_SECONDS seconds")
            // An upper waiting time bound can be set up,
            // but this bound may be overly pessimistic.
            Thread.sleep(CIRCUITMANAGER_MAX_SETUP_WAIT_TIME_SECONDS.toLong())
        }

        if (CircuitManager[circuit] == CircuitManager.Status.Outdated) {
            log.debug("Circuit outdated, cleaning up")
            zkService.cleanup()
            CircuitManager.inProgress(circuit)
            log.debug("Circuit outdated, starting setup")
            zkService.setupTimed()
            log.debug("Circuit setup complete, caching it")
            CircuitManager.cache(circuit)
        }
    }

    fun cleanup(command: ResolvedZKCommandMetadata) = zkServiceForCommandMetadata(command).cleanup()
}
