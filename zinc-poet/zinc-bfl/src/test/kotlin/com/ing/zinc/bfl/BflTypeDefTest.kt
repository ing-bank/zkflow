package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateEmptyCircuit
import com.ing.zinc.bfl.ZincExecutor.generateEqualsCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class BflTypeDefTest {
    companion object {
        val typeDefBool = BflTypeDef("MyTypeDefBool", BflPrimitive.Bool)
        val typeDefStruct = BflTypeDef("MyTypeDefStruct", structWithPrimitiveFields)
    }

    @Test
    fun `type definition of primitive should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(typeDefBool)
        tempDir.generateWitness(SERIALIZED) { bits(1) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `type definition of primitive should return correct default`(@TempDir tempDir: Path) {
        tempDir.generateEmptyCircuit(typeDefBool)

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `type definition of primitive should detect different inputs`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(typeDefBool)
        tempDir.generateWitness {
            put("left", JsonPrimitive(false))
            put("right", JsonPrimitive(true))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `type definition of primitive should detect equal inputs`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(typeDefBool)
        tempDir.generateWitness {
            put("left", JsonPrimitive(true))
            put("right", JsonPrimitive(true))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `type definition of module should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(typeDefStruct)
        tempDir.generateWitness(SERIALIZED) {
            bytes(1, 0, 0, 0)
            bits(1)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe primitiveStructJson(true, 16777216)
    }

    @Test
    fun `type definition of module should return correct default`(@TempDir tempDir: Path) {

        tempDir.generateEmptyCircuit(typeDefStruct)

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe primitiveStructJson(false, 0)
    }

    @Test
    fun `type definition of module should detect different inputs`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(typeDefStruct)
        tempDir.generateWitness {
            put("left", primitiveStructJson(false, 0))
            put("right", primitiveStructJson(true, 1))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `type definition of module should detect equal inputs`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(typeDefStruct)
        tempDir.generateWitness {
            put("left", primitiveStructJson(false, 0))
            put("right", primitiveStructJson(false, 0))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }
}
