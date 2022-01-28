package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.util.runCommand
import com.ing.zkflow.zinc.poet.generate.types.CommandGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.LedgerTransactionFactory
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.COMMANDS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.NOTARY
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PARAMETERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_INPUT_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_REFERENCE_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal class CircuitGeneratorTest {
    private val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)

    private val standardTypes = StandardTypes(zincTypeResolver)

    private val commandGroupFactory = CommandGroupFactory(standardTypes)

    @ExperimentalTime
    @Test
    @Tag("slow")
    fun `generateCircuitFor should generate a working circuit`(@TempDir tempDir: Path) {
        val circuitGenerator = CircuitGenerator(
            BuildPathProvider.withPath(tempDir),
            LedgerTransactionFactory(commandGroupFactory, standardTypes),
            standardTypes,
            StateAndRefsGroupFactory(standardTypes),
            zincTypeResolver,
            ConstsFactory(),
            CryptoUtilsFactory(),
        )

        logExecutionTime("Generating the circuit") {
            circuitGenerator.generateCircuitFor(MyContract.MyFirstCommand().metadata)
        }

        val result = logExecutionTime("Running the circuit using `zargo run`") {
            tempDir.runCommand("zargo run", 120)
        }

        result.second shouldBe ""
        val publicInput = Json.parseToJsonElement(result.first) as JsonObject

        publicInput.shouldContainKeys(
            INPUTS, OUTPUTS, REFERENCES, SERIALIZED_INPUT_UTXOS, SERIALIZED_REFERENCE_UTXOS, COMMANDS, PARAMETERS, SIGNERS, NOTARY
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
