package com.ing.zkflow.testing.dsl.services

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.collectUtxoInfos
import com.ing.zkflow.common.transactions.commandData
import com.ing.zkflow.common.zkp.PublicInput
import com.ing.zkflow.common.zkp.Witness
import com.ing.zkflow.common.zkp.ZincZKTransactionService
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.zinc.poet.generate.BuildPathProvider
import com.ing.zkflow.zinc.poet.generate.CircuitGenerator
import com.ing.zkflow.zinc.poet.generate.ConstsFactory
import com.ing.zkflow.zinc.poet.generate.CryptoUtilsFactory
import com.ing.zkflow.zinc.poet.generate.ZincTypeGenerator
import com.ing.zkflow.zinc.poet.generate.ZincTypeGeneratorResolver
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.deleteRecursively
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import java.nio.file.Files
import java.nio.file.Path

@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class TestDSLZincZKTransactionService(serviceHub: ServiceHub) : TestDSLZKTransactionService, ZincZKTransactionService(serviceHub) {
    public override fun calculatePublicInput(tx: ZKVerifierTransaction, commandMetadata: ResolvedZKCommandMetadata): PublicInput {
        // Fetch the UTXO hashes from the svtx's pointed to by the inputs and references.
        // This confirms that we have a validated backchain stored for them.
        val privateInputHashes = tx.inputs.filterIndexed { index, _ ->
            commandMetadata.isVisibleInWitness(ComponentGroupEnum.INPUTS_GROUP.ordinal, index)
        }.let { getUtxoHashes(it, tx.digestService) }

        val privateReferenceHashes = tx.references.filterIndexed { index, _ ->
            commandMetadata.isVisibleInWitness(ComponentGroupEnum.REFERENCES_GROUP.ordinal, index)
        }.let { privateStateRefs -> getUtxoHashes(privateStateRefs, tx.digestService) }

        // Fetch output component hashes for private outputs of the command
        val privateOutputHashes = tx.outputHashes().filterIndexed { index, _ ->
            commandMetadata.isVisibleInWitness(ComponentGroupEnum.OUTPUTS_GROUP.ordinal, index)
        }

        return PublicInput(
            outputComponentHashes = privateOutputHashes,
            attachmentComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal),
            commandComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.COMMANDS_GROUP.ordinal),
            notaryComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.NOTARY_GROUP.ordinal),
            parametersComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.PARAMETERS_GROUP.ordinal),
            timeWindowComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal),
            signersComponentHashes = tx.privateComponentHashes(ComponentGroupEnum.SIGNERS_GROUP.ordinal),

            inputUtxoHashes = privateInputHashes,
            referenceUtxoHashes = privateReferenceHashes
        )
    }

    private fun getUtxoHashes(stateRefs: List<StateRef>, digestService: DigestService): List<SecureHash> =
        serviceHub.collectUtxoInfos(stateRefs).map { digestService.componentHash(it.nonce, OpaqueBytes(it.serializedContents)) }

    public override fun verify(wtx: WireTransaction, zkNetworkParameters: ZKNetworkParameters): SignedZKVerifierTransaction {
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
}
