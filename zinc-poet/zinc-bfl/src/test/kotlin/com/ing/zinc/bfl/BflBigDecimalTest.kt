package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.createImports
import com.ing.zinc.bfl.ZincExecutor.generateCircuitBase
import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateEqualsCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.zinc.types.asZincJsonNumberList
import com.ing.zkflow.zinc.types.parseJson
import com.ing.zkflow.zinc.types.toJsonObject
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Path

private fun BflBigDecimal.generateJsonBigDecimalZero() = buildJsonObject {
    put("kind", JsonPrimitive("0"))
    put("sign", JsonPrimitive("0"))
    put("integer", emptyList<Int>().asZincJsonNumberList(this@generateJsonBigDecimalZero.integerSize, 0))
    put("fraction", emptyList<Int>().asZincJsonNumberList(this@generateJsonBigDecimalZero.fractionSize, 0))
}

internal class BflBigDecimalTest {
    @Test
    fun `BflBigDecimal deserialize method should deserialize correctly with witness 1`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(module)
        tempDir.generateWitness(SERIALIZED) {
            bytes(3, 255) // kind: 0, sign: -1
            bytes(0, 0, 0, 12) // integer size: 12
            bytes(3, 2, 1) // integer: 123
            bytes(*IntArray(9)) // integer padding most significant bytes
            bytes(0, 0, 0, 4) // fraction size: 4
            bytes(1, 2, 5, 0) // fraction: 0.125
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe BigDecimal("-123.125").toJsonObject(module)
    }

    @Test
    fun `BflBigDecimal compare should detect this equals that`(@TempDir tempDir: Path) {
        tempDir.generateCompareCircuit(module)
        tempDir.generateWitness {
            put("this", BigDecimal("123.456").toJsonObject(module))
            put("that", BigDecimal("123.456").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("0")
    }

    @Test
    fun `BflBigDecimal compare should detect this is larger than that`(@TempDir tempDir: Path) {
        tempDir.generateCompareCircuit(module)
        tempDir.generateWitness {
            put("this", BigDecimal("123.456").toJsonObject(module))
            put("that", BigDecimal("23.456").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("1")
    }

    @Test
    fun `BflBigDecimal compare should detect this is smaller than that`(@TempDir tempDir: Path) {
        tempDir.generateCompareCircuit(module)
        tempDir.generateWitness {
            put("this", BigDecimal("123.456").toJsonObject(module))
            put("that", BigDecimal("1023.456").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("-1")
    }

    @Test
    fun `BflBigDecimal plus should work correctly`(@TempDir tempDir: Path) {
        tempDir.generatePlusCircuit(module)
        tempDir.generateWitness {
            put("this", BigDecimal("123.456").toJsonObject(module))
            put("that", BigDecimal("123.456").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe BigDecimal("246.912").toJsonObject(module)
    }

    @Test
    fun `BflBigDecimal plus should work correctly with carry`(@TempDir tempDir: Path) {
        tempDir.generatePlusCircuit(module)
        tempDir.generateWitness {
            put("this", BigDecimal("445.0555").toJsonObject(module))
            put("that", BigDecimal("555.0445").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe BigDecimal("1000.1").toJsonObject(module)
    }

    @Test
    fun `BflBigDecimal minus should work correctly`(@TempDir tempDir: Path) {
        tempDir.generateMinusCircuit(module)
        tempDir.generateWitness {
            put("this", BigDecimal("123.456").toJsonObject(module))
            put("that", BigDecimal("123.456").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe module.generateJsonBigDecimalZero()
    }

    @Test
    fun `BflBigDecimal minus should work correctly 2`(@TempDir tempDir: Path) {
        tempDir.generateMinusCircuit(module)
        tempDir.generateWitness {
            put("this", BigDecimal("123").toJsonObject(module))
            put("that", BigDecimal("0.5").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe BigDecimal("122.5").toJsonObject(module)
    }

    @Test
    fun `BflBigDecimal equals should detect equal numbers`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(module)
        tempDir.generateWitness {
            put("left", BigDecimal("123123").toJsonObject(module))
            put("right", BigDecimal("000123123").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `BflBigDecimal equals should detect numbers differing in integer`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(module)
        tempDir.generateWitness {
            put("left", BigDecimal("123.123").toJsonObject(module))
            put("right", BigDecimal("123.0123").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `BflBigDecimal equals should detect numbers differing in fraction`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(module)
        tempDir.generateWitness {
            put("left", BigDecimal("123.123").toJsonObject(module))
            put("right", BigDecimal("1230.0123").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `BflBigDecimal equals should detect numbers differing in sign`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(module)
        tempDir.generateWitness {
            put("left", BigDecimal("123.123").toJsonObject(module))
            put("right", BigDecimal("-123.123").toJsonObject(module))
        }
        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    companion object {
        val module = BflBigDecimal(12, 4)

        private fun Path.generateCompareCircuit(module: BflBigDecimal) {
            generateCircuitBase(module)
            // generate src/main.zn
            zincSourceFile("main.zn") {
                module.allModules {
                    createImports(this)
                }
                function {
                    name = "main"
                    parameter {
                        name = "this"
                        type = module.toZincId()
                    }
                    parameter {
                        name = "that"
                        type = module.toZincId()
                    }
                    returnType = ZincPrimitive.I8
                    body = "this.compare(that)"
                }
            }
        }

        private fun Path.generatePlusCircuit(module: BflBigDecimal) {
            generateCircuitBase(module)
            // generate src/main.zn
            zincSourceFile("main.zn") {
                module.allModules {
                    createImports(this)
                }
                function {
                    name = "main"
                    parameter {
                        name = "this"
                        type = module.toZincId()
                    }
                    parameter {
                        name = "that"
                        type = module.toZincId()
                    }
                    returnType = module.toZincId()
                    body = "this.plus(that)"
                }
            }
        }

        private fun Path.generateMinusCircuit(module: BflBigDecimal) {
            generateCircuitBase(module)
            // generate src/main.zn
            zincSourceFile("main.zn") {
                module.allModules {
                    createImports(this)
                }
                function {
                    name = "main"
                    parameter {
                        name = "this"
                        type = module.toZincId()
                    }
                    parameter {
                        name = "that"
                        type = module.toZincId()
                    }
                    returnType = module.toZincId()
                    body = "this.minus(that)"
                }
            }
        }
    }
}
