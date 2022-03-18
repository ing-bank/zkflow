package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
import com.ing.zkflow.util.runCommand
import com.ing.zkflow.zinc.poet.generate.types.CommandContextFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_INPUT_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_REFERENCE_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.witness.toPublicInputFieldName
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal class CircuitGeneratorTest {
    private val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)

    private val standardTypes = StandardTypes(MockZKNetworkParameters())

    @ExperimentalTime
    @Test
    @Tag("slow")
    // fun `generateCircuitFor should generate a working circuit`(@TempDir tempDir: Path) {
    fun `generateCircuitFor should generate a working circuit`() {
        val tempDir = Files.createTempDirectory(this::class.simpleName)
        val circuitGenerator = CircuitGenerator(
            BuildPathProvider.withPath(tempDir),
            CommandContextFactory(standardTypes),
            standardTypes,
            zincTypeResolver,
            ConstsFactory(),
            CryptoUtilsFactory()
        )

        logExecutionTime("Generating the circuit") {
            circuitGenerator.generateCircuitFor(MyContract.MyFirstCommand())
        }

        val result = logExecutionTime("Running the circuit using `zargo run`") {
            // This needs a crazy long timeout so it doesn't fail on CI with parallel builds.
            tempDir.runCommand("zargo run", 800)
        }

        result.second shouldBe ""
        val publicInput = Json.parseToJsonElement(result.first) as JsonObject

        publicInput.keys.shouldContainAll(
            listOf(OUTPUTS, SERIALIZED_INPUT_UTXOS, SERIALIZED_REFERENCE_UTXOS)
                .map { it.toPublicInputFieldName() }
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CircuitGeneratorTest::class.java)

        @ExperimentalTime
        private fun <T> logExecutionTime(message: String, block: () -> T): T {
            logger.info("Started: $message")
            val out: T
            val time = measureTime {
                out = block()
            }
            logger.info("Finished: $message in $time")
            return out
        }
    }
}
