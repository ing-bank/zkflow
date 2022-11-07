package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
import com.ing.zkflow.util.measureTimedValue
import com.ing.zkflow.util.runCommand
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.COMMANDS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.NOTARY
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PARAMETERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_INPUT_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_REFERENCE_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.TIME_WINDOW
import com.ing.zkflow.zinc.poet.generate.types.witness.toPublicInputFieldName
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

internal class CircuitGeneratorTest {
    private val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)

    private val standardTypes = StandardTypes(MockZKNetworkParameters())

    @Test
    @Tag("slow")
    fun `generateCircuitFor should generate a working circuit for MyFirstCommand`(@TempDir tempDir: Path) {
        val (stdout, stderr) = generateAndRunCircuit(tempDir, MyContract.MyFirstCommand())

        stderr shouldBe ""
        val publicInput = Json.parseToJsonElement(stdout) as JsonObject

        publicInput.keys.shouldContainAll(
            listOf(OUTPUTS, SERIALIZED_INPUT_UTXOS, SERIALIZED_REFERENCE_UTXOS, TIME_WINDOW, PARAMETERS, NOTARY, COMMANDS)
                .map { it.toPublicInputFieldName() }
        )
    }

    @Test
    @Tag("slow")
    fun `generateCircuitFor should generate a working circuit for UpgradeMyStateV1ToMyStateV2`(@TempDir tempDir: Path) {
        val (_, stderr) = generateAndRunCircuit(tempDir, MyContract.UpgradeMyStateV1ToMyStateV2())

        // NOTE that here we know that it works, because the automatically generated witness does not have the right
        // default value for the count field.
        stderr shouldContain "[UpgradeMyStateV1ToMyStateV2] Not a valid upgrade from MyStateV1 to MyStateV2."
    }

    private fun generateAndRunCircuit(
        tempDir: Path,
        zkCommand: ZKCommandData
    ): Pair<String, String> {
        val circuitGenerator = getCircuitGenerator(tempDir)

        logExecutionTime("Generating the circuit") {
            circuitGenerator.generateCircuitFor(zkCommand)
        }

        val result = logExecutionTime("Running the circuit using `zargo run`") {
            // This needs a crazy long timeout, so it doesn't fail on CI with parallel builds.
            tempDir.runCommand("zargo run", 800)
        }
        return result
    }

    private fun getCircuitGenerator(tempDir: Path) = CircuitGenerator(
        BuildPathProvider.withPath(tempDir),
        CommandContextFactory(standardTypes),
        standardTypes,
        zincTypeResolver,
        ConstsFactory,
        CryptoUtilsFactory
    )

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CircuitGeneratorTest::class.java)

        private fun <T> logExecutionTime(message: String, block: () -> T): T {
            logger.info("Started: $message")
            val timedValue = measureTimedValue {
                block()
            }
            logger.info("Finished: $message in ${timedValue.duration}")
            return timedValue.value
        }
    }
}
