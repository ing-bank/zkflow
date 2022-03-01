package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class BflEnumTest {
    @Test
    fun `Enum should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(thingsEnum)
        tempDir.generateWitness(SERIALIZED) { bytes(0, 0, 0, 3) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("3")
    }

    @Test
    fun `Enum should fail to deserialize for incorrect value`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(thingsEnum)
        tempDir.generateWitness(SERIALIZED) { bytes(0, 0, 0, 4) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldContain "Not a Things"
        stdout shouldBe ""
    }
}
