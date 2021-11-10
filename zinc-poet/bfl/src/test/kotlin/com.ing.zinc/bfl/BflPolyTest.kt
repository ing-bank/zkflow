package com.ing.zinc.bfl

import com.ing.zinc.bfl.DataInterface.Companion.polyIntData
import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommand
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class BflPolyTest {
    @Test
    fun `Poly should be deserialized correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(polyIntData)
        tempDir.generateWitness(SERIALIZED) {
            bytes(
                0, 1, 0, 'a'.toInt(),
                0, 0, 0, 47
            )
        }

        val (stdout, stderr) = tempDir.runCommand("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe buildJsonObject {
            put("inner", buildJsonObject { put("value", "47") })
            put("serial_name", "a".asZincJsonString(1))
        }
    }
}
