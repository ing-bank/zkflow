package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransactionWithoutProofs
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.transactions.commandData
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZincZKTransactionService
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.zinc.poet.generate.BuildPathProvider
import com.ing.zkflow.zinc.poet.generate.CircuitGenerator
import com.ing.zkflow.zinc.poet.generate.ConstsFactory
import com.ing.zkflow.zinc.poet.generate.CryptoUtilsFactory
import com.ing.zkflow.zinc.poet.generate.ZincTypeGenerator
import com.ing.zkflow.zinc.poet.generate.ZincTypeGeneratorResolver
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.corda.core.internal.deleteRecursively
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.WireTransaction
import java.nio.file.Files
import java.nio.file.Path

@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class TestDSLZincZKTransactionService(serviceHub: ServiceHub, zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage) :
    TestDSLZKTransactionService, ZincZKTransactionService(serviceHub) {
    override val vtxStorage: ZKWritableVerifierTransactionStorage = zkVerifierTransactionStorage

    /**
     * This verification function should do all the same checks as `AbstractZKTransactionService.verify(svtx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean)`.
     * This ensures that DSL tests validate transactions consistently with the real ZKTransactionService used.
     */
    public override fun verify(wtx: WireTransaction, zkNetworkParameters: ZKNetworkParameters): SignedZKVerifierTransaction {
        val vtx = ZKVerifierTransactionWithoutProofs.fromWireTransaction(wtx)

        // Verify the ZKPs for all ZKCommandDatas in this transaction
        verifyProofs(wtx, zkNetworkParameters, vtx)

        return SignedZKVerifierTransaction(vtx)
    }

    private fun verifyProofs(
        wtx: WireTransaction,
        zkNetworkParameters: ZKNetworkParameters,
        vtx: ZKVerifierTransaction
    ) {
        wtx.commandData.forEach { command ->
            val temporaryCircuitBuildFolder = Files.createTempDirectory(command.metadata.circuit.name)

            val commandMetadata = command.metadata.copy(
                circuit = command.metadata.circuit.copy(
                    buildFolder = temporaryCircuitBuildFolder.toFile()
                )
            )

            generateTemporaryCircuit(zkNetworkParameters, command, temporaryCircuitBuildFolder)
            val witness = Witness.fromWireTransaction(
                wtx = wtx,
                inputUtxoInfos = serviceHub.collectUtxoInfos(wtx.inputs),
                referenceUtxoInfos = serviceHub.collectUtxoInfos(wtx.references),
                commandMetadata
            )

            zkServiceForCommandMetadata(commandMetadata).run(witness, calculatePublicInput(vtx, commandMetadata))

            // TODO Don't delete when debugging, do delete otherwise (flag in ZKNetworkParameters, or System Property?)
            temporaryCircuitBuildFolder.deleteRecursively()
        }
    }

    private fun generateTemporaryCircuit(
        zkNetworkParameters: ZKNetworkParameters,
        command: ZKCommandData,
        temporaryCircuitBuildFolder: Path
    ) {
        val standardTypes = StandardTypes(zkNetworkParameters)
        val circuitGenerator = CircuitGenerator(
            BuildPathProvider.withPath(temporaryCircuitBuildFolder),
            CommandContextFactory(standardTypes),
            standardTypes,
            ZincTypeGeneratorResolver(ZincTypeGenerator),
            ConstsFactory,
            CryptoUtilsFactory
        )
        circuitGenerator.generateCircuitFor(command)
    }
}
