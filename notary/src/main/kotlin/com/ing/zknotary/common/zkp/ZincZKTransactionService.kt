package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.node.services.ConfigParams
import com.ing.zknotary.node.services.getLongFromConfig
import com.ing.zknotary.node.services.getStringFromConfig
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import java.io.File
import java.time.Duration

@CordaService
class ZincZKTransactionService(services: AppServiceHub) : ZKTransactionCordaService(services) {

    private val zkServices: Map<ZKCommandData, ZincZKService>

    init {

        val buildTimeout: Duration = Duration.ofSeconds(services.getLongFromConfig(ConfigParams.Zinc.BUILD_TIMEOUT, 60))
        val setupTimeout: Duration = Duration.ofSeconds(services.getLongFromConfig(ConfigParams.Zinc.SETUP_TIMEOUT, 60))
        val provingTimeout: Duration = Duration.ofSeconds(services.getLongFromConfig(ConfigParams.Zinc.PROVING_TIMEOUT, 60))
        val verificationTimeout: Duration = Duration.ofSeconds(services.getLongFromConfig(ConfigParams.Zinc.VERIFICATION_TIMEOUT, 60))

        val commandClasses = services.getStringFromConfig(ConfigParams.Zinc.COMMAND_CLASS_NAMES).split(ConfigParams.Zinc.COMMANDS_SEPARATOR)

        // Probably can be not that strict
        if (commandClasses.isEmpty()) throw ExceptionInInitializerError("List of ZK Commands cannot be empty")

        val commands: List<ZKCommandData> = commandClasses.map { Class.forName(it).getConstructor().newInstance() as ZKCommandData }

        zkServices = commands.map {

            val circuitFolder = it.circuit.folder
            val artifactFolder = File(circuitFolder, "artifacts")

            val zkService = ZincZKService(circuitFolder.absolutePath, artifactFolder.absolutePath, buildTimeout, setupTimeout, provingTimeout, verificationTimeout)

            it to zkService
        }.toMap()
    }

    override fun zkServiceForTx(command: ZKCommandData): ZKService {
        return zkServices[command] ?: throw IllegalArgumentException("ZK Service not found for circuitId $command")
    }

    fun setup(force: Boolean = false) {

        if (force) {
            cleanup()
        }

        zkServices.values.forEach { zkService ->

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
    }

    fun cleanup() = zkServices.values.forEach { it.cleanup() }
}
