package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.createImports
import com.ing.zinc.bfl.ZincExecutor.generateCircuitBase
import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateEqualsCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zkflow.zinc.types.parseJson
import com.ing.zkflow.zinc.types.zincJsonOptionOf
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path

internal class BflOptionTest {
    @Test
    fun `Option of bool should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(booleanOption)
        tempDir.generateWitness(SERIALIZED) {
            bits(
                1, // true
                0 // false
            )
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe zincJsonOptionOf(JsonPrimitive(false))
    }

    @Test
    fun `Option of i8 should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(intOption)
        tempDir.generateWitness(SERIALIZED) {
            bits(1) // true
            bytes(255) // -1
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe zincJsonOptionOf(JsonPrimitive("-1"))
    }

    @ParameterizedTest
    @ValueSource(ints = [42, 13])
    fun `Option of i8 should create a valid option of some number`(value: Int, @TempDir tempDir: Path) {
        tempDir.generateSomeCircuit(intOption)
        tempDir.generateWitness { put("some_value", "$value") }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe zincJsonOptionOf(JsonPrimitive("$value"))
    }

    @Test
    fun `Option of i8 should be equal when values are absent`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(intOption)
        tempDir.generateWitness {
            put("left", zincJsonOptionOf(JsonPrimitive("0"), false))
            put("right", zincJsonOptionOf(JsonPrimitive("1"), false))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `Option of i8 should not be equal when one value absent`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(intOption)
        tempDir.generateWitness {
            put("left", zincJsonOptionOf(JsonPrimitive("0"), false))
            put("right", zincJsonOptionOf(JsonPrimitive("1")))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `Option of i8 should not be equal when different values present`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(intOption)
        tempDir.generateWitness {
            put("left", zincJsonOptionOf(JsonPrimitive("0")))
            put("right", zincJsonOptionOf(JsonPrimitive("1")))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `Option of i8 should be equal when same values present`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(intOption)
        tempDir.generateWitness {
            put("left", zincJsonOptionOf(JsonPrimitive("1")))
            put("right", zincJsonOptionOf(JsonPrimitive("1")))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    private fun Path.generateSomeCircuit(option: BflOption) {
        generateCircuitBase(option)
        zincSourceFile("main.zn") {
            createImports(option)
            function {
                name = "main"
                parameter { name = "some_value"; type = option.innerType.toZincId() }
                returnType = option.toZincId()
                body = "${option.id}::some(some_value)"
            }
        }
    }
}
