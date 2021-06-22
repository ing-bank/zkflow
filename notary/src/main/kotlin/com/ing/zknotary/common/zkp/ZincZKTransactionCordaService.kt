package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.node.services.ConfigParams
import com.ing.zknotary.node.services.getLongFromConfig
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import java.io.File
import java.time.Duration

@CordaService
class ZincZKTransactionCordaService(services: AppServiceHub) : ZincZKTransactionService(services)

@CordaService
open class ZincZKTransactionService(services: ServiceHub) : AbstractZKTransactionService(services) {

    private val zkServices = mutableMapOf<ZKCommandData, ZincZKService>()
    val buildTimeout: Duration = Duration.ofSeconds(services.getLongFromConfig(ConfigParams.Zinc.BUILD_TIMEOUT, 60))
    val setupTimeout: Duration = Duration.ofSeconds(services.getLongFromConfig(ConfigParams.Zinc.SETUP_TIMEOUT, 60))
    val provingTimeout: Duration = Duration.ofSeconds(services.getLongFromConfig(ConfigParams.Zinc.PROVING_TIMEOUT, 60))
    val verificationTimeout: Duration = Duration.ofSeconds(services.getLongFromConfig(ConfigParams.Zinc.VERIFICATION_TIMEOUT, 60))

    override fun zkServiceForCommand(command: ZKCommandData): ZincZKService {
        return zkServices.getOrPut(command) {
            val circuitFolder = command.circuit.folder
            val artifactFolder = File(circuitFolder, "artifacts")

            return ZincZKService(
                circuitFolder.absolutePath,
                artifactFolder.absolutePath,
                buildTimeout,
                setupTimeout,
                provingTimeout,
                verificationTimeout
            )
        }
    }

    fun setup(command: ZKCommandData, force: Boolean = false) {

        if (force) {
            cleanup(command)
        }

        val zkService = zkServiceForCommand(command)

        val circuit = CircuitManager.CircuitDescription("${zkService.circuitFolder}/src", zkService.artifactFolder)
        CircuitManager.register(circuit)

        while (CircuitManager[circuit] == CircuitManager.Status.InProgress) {
            // An upper waiting time bound can be set up,
            // but this bound may be overly pessimistic.
            Thread.sleep(10 * 1000)
        }

        if (CircuitManager[circuit] == CircuitManager.Status.Outdated) {
            zkService.cleanup()
            CircuitManager.inProgress(circuit)
            zkService.setup()
            CircuitManager.cache(circuit)
        }
    }

    fun cleanup(command: ZKCommandData) = zkServiceForCommand(command).cleanup()
}
