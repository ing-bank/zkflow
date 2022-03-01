package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.createImports
import com.ing.zinc.bfl.ZincExecutor.generateCircuitBase
import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateEmptyCircuit
import com.ing.zinc.bfl.ZincExecutor.generateEqualsCircuit
import com.ing.zinc.bfl.ZincExecutor.generateNewCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class BflStructTest {
    @Test
    fun `BflStruct deserialize method should deserialize correctly with witness 1`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(structWithPrimitiveFields)
        tempDir.generateWitness(SERIALIZED) {
            bytes(1, 0, 0, 0)
            bits(1)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe primitiveStructJson(true, 16777216)
    }

    @Test
    fun `BflStruct deserialize method should deserializes correctly with witness 2`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(structWithPrimitiveFields)
        tempDir.generateWitness(SERIALIZED) {
            bytes(0, 0, 0, 0)
            bits(0)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe primitiveStructJson(false, 0)
    }

    @Test
    fun `BflStruct new method should instantiate a correct struct`(@TempDir tempDir: Path) {
        tempDir.generateNewCircuit(structWithPrimitiveFields, "35 as u32, true")

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe primitiveStructJson(true, 35)
    }

    @Test
    fun `BflStruct empty method should instantiate an empty struct`(@TempDir tempDir: Path) {
        tempDir.generateEmptyCircuit(structWithPrimitiveFields)

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe primitiveStructJson(false, 0)
    }

    @Test
    fun `BflStruct equals method should detect equal structs`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(structWithPrimitiveFields)
        tempDir.generateWitness {
            put("left", primitiveStructJson(false, 0))
            put("right", primitiveStructJson(false, 0))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `BflStruct equals method should detect different structs`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(structWithPrimitiveFields)
        tempDir.generateWitness {
            put("left", primitiveStructJson(false, 0))
            put("right", primitiveStructJson(true, 1))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `BflStruct deserialize method should deserialize correctly with arrays`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(structWithArrayFieldsOfPrimitives)
        tempDir.generateWitness(SERIALIZED) {
            bytes(
                0, 0, 0, 5, // 5
                0, 0, 0, 12, // 12
            )
            bits(
                0, // false
                1, // true
            )
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe arrayStructJson(listOf(false, true), listOf(5, 12))
    }

    @Test
    fun `BflStruct new method should instantiate a correct struct with arrays`(@TempDir tempDir: Path) {
        tempDir.generateNewCircuit(structWithArrayFieldsOfPrimitives, "[35 as u32, 13 as u32], [true, false]")

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe arrayStructJson(listOf(true, false), listOf(35, 13))
    }

    @Test
    fun `BflStruct empty method should instantiate an empty struct with arrays`(@TempDir tempDir: Path) {
        tempDir.generateEmptyCircuit(structWithArrayFieldsOfPrimitives)

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe arrayStructJson(listOf(false, false), listOf(0, 0))
    }

    @Test
    fun `BflStruct equals method should detect equal structs with arrays`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(structWithArrayFieldsOfPrimitives)
        tempDir.generateWitness {
            put("left", arrayStructJson(listOf(false, false), listOf(0, 0)))
            put("right", arrayStructJson(listOf(false, false), listOf(0, 0)))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `BflStruct equals method should detect different structs with arrays`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(structWithArrayFieldsOfPrimitives)
        tempDir.generateWitness {
            put("left", arrayStructJson(listOf(false, false), listOf(0, 0)))
            put("right", arrayStructJson(listOf(false, false), listOf(0, 1)))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `BflStruct deserialize method should deserialize correctly with struct`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(structWithStructField)
        tempDir.generateWitness(SERIALIZED) {
            bytes(1, 0, 0, 0)
            bits(1)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe structStructJson(true, 16777216)
    }

    @Test
    fun `BflStruct new method should instantiate a correct struct with struct`(@TempDir tempDir: Path) {
        tempDir.generateNewCircuit(structWithStructField, "${structWithPrimitiveFields.id}::new(35 as u32, true)")

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe structStructJson(true, 35)
    }

    @Test
    fun `BflStruct empty method should instantiate an empty struct with struct`(@TempDir tempDir: Path) {
        tempDir.generateEmptyCircuit(structWithStructField)

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe structStructJson(false, 0)
    }

    @Test
    fun `BflStruct equals method should detect equal structs with struct`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(structWithStructField)
        tempDir.generateWitness {
            put("left", structStructJson(false, 0))
            put("right", structStructJson(false, 0))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `BflStruct equals method should detect different structs with struct`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(structWithStructField)
        tempDir.generateWitness {
            put("left", structStructJson(false, 0))
            put("right", structStructJson(true, 1))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `BflStruct should generate additional method for StructWithArrayFields`(@TempDir tempDir: Path) {
        tempDir.generateFooCircuit(structWithArrayFieldsOfPrimitives)
        tempDir.generateWitness {
            put("this", arrayStructJson(listOf(false, true), listOf(0, 0)))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    companion object {
        init {
            BflModule.registerMethod(
                "StructWithArrayFields",
                zincMethod {
                    name = "foo"
                    returnType = ZincPrimitive.Bool
                    body = "self.bar[0] || self.bar[1]"
                }
            )
        }

        fun Path.generateFooCircuit(module: BflStruct) {
            generateCircuitBase(module) // generate src/main.zn
            zincSourceFile("main.zn") {
                module.allModules {
                    createImports(this)
                }
                function {
                    name = "main"
                    parameter { name = "this"; type = module.toZincId() }
                    returnType = ZincPrimitive.Bool
                    body = "this.foo()"
                }
            }
        }
    }
}
