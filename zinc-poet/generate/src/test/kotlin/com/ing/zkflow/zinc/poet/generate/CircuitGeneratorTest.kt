package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.util.runCommand
import com.ing.zkflow.zinc.poet.generate.types.CommandGroupFactory.COMMAND
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal class CircuitGeneratorTest {
    private val zincTypeResolver = object : ZincTypeResolver {
        override fun zincTypeOf(kClass: KClass<*>): BflModule {
            return struct {
                name = kClass.simpleName
                // Just a random field, to make zinc happy. Zinc is not so good with empty structs.
                field {
                    name = "bogus"
                    type = BflPrimitive.U8
                }
            }
        }
    }

    @ExperimentalTime
    @Test
    @Tag("slow")
    fun `generateCircuitFor should generate a working circuit`(@TempDir tempDir: Path) {
        val circuitGenerator = CircuitGenerator(zincTypeResolver, BuildPathProvider.withPath(tempDir))

        logExecutionTime("Generating the circuit") {
            circuitGenerator.generateCircuitFor(MyContract.MyFirstCommand())
        }

        val result = logExecutionTime("Running the circuit using `zargo run`") {
            tempDir.runCommand("zargo run", 60)
        }

        result.second shouldBe ""
        result.first shouldContain MyContract.MyFirstCommand().metadata.commandSimpleName.removeSuffix(COMMAND).camelToSnakeCase()
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CircuitGeneratorTest::class.java)

        @ExperimentalTime
        private fun <T> logExecutionTime(message: String, block: () -> T): T {
            val out: T
            val time = measureTime {
                out = block()
            }
            logger.info("$message took $time")
            return out
        }
    }
}
