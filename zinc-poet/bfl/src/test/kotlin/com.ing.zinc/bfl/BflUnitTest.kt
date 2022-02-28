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

internal class BflUnitTest {
    @Test
    fun `BflUnit deserializes correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(BflUnit)
        tempDir.generateWitness(SERIALIZED) { }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("unit")
    }

    @Test
    fun `BflUnit empty method should instantiate BflUnit`(@TempDir tempDir: Path) {
        tempDir.generateEmptyCircuit(BflUnit)

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("unit")
    }

    @Test
    fun `BflUnit equals method always returns true`(@TempDir tempDir: Path) {
        tempDir.generateEqualsCircuit(BflUnit)
        tempDir.generateWitness {
            put("left", JsonPrimitive("unit"))
            put("right", JsonPrimitive("unit"))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }
}
