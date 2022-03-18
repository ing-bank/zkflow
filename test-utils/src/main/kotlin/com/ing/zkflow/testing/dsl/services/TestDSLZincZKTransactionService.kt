package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.transactions.commandData
import com.ing.zkflow.common.transactions.zkTransactionMetadata
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZincZKTransactionService
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.testing.dsl.interfaces.VerificationMode
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
public class TestDSLZincZKTransactionService(serviceHub: ServiceHub) : TestDSLZKTransactionService, ZincZKTransactionService(serviceHub) {
    public override fun calculatePublicInput(tx: ZKVerifierTransaction, commandMetadata: ResolvedZKCommandMetadata): PublicInput =
        calculatePublicInput(serviceHub, tx, commandMetadata)

    override fun run(wtx: WireTransaction, zkNetworkParameters: ZKNetworkParameters): SignedZKVerifierTransaction {
        val proofs = mutableMapOf<String, ByteArray>()
        val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

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
        return SignedZKVerifierTransaction(vtx)
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
            ConstsFactory(),
            CryptoUtilsFactory()
        )
        circuitGenerator.generateCircuitFor(command)
    }

    public override fun verify(
        wtx: WireTransaction,
        zkNetworkParameters: ZKNetworkParameters,
        mode: VerificationMode
    ): SignedZKVerifierTransaction {
        return when (mode) {
            VerificationMode.RUN -> run(wtx, zkNetworkParameters)
            VerificationMode.PROVE_AND_VERIFY -> {
                throw UnsupportedOperationException("This is not currently supported on DSL Contract tests")
                // proveVerify(wtx)
            }
            VerificationMode.MOCK -> {
                TestDSLMockZKTransactionService(serviceHub).run(wtx, zkNetworkParameters)
            }
        }
    }

    @Suppress("unused", "UnusedPrivateMember[s")
    private fun proveVerify(wtx: WireTransaction): SignedZKVerifierTransaction {
        wtx.zkTransactionMetadata().commands.forEach {
            setup(it) // Should be idempotent
        }
        val svtx = SignedZKVerifierTransaction(prove(wtx))
        verify(svtx, false)
        return svtx
    }
}
