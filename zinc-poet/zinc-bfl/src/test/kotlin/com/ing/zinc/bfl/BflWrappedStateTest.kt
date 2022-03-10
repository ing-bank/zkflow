package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.generateDeserializeLastFieldCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.bfl.dsl.WrappedStateBuilder.Companion.wrappedState
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class BflWrappedStateTest {
    @Test
    fun `BflWrappedField should be successfully deserialized into the state field`(@TempDir tempDir: Path) {
        val actual = wrappedState {
            name = "Thingy"
            cordaMagic()
            metadata(
                struct {
                    name = "TypeId"
                    field {
                        name = "type_id"
                        type = BflPrimitive.I32
                    }
                }
            )
            state(BflPrimitive.U8)
        }
        tempDir.generateDeserializeLastFieldCircuit(actual)
        tempDir.generateWitness(SERIALIZED) {
            bytes(0, 0, 0, 0, 0, 0, 0) // [i8; 7] - Corda magic
            bytes(0, 0, 0, 0) // i32 - Type id
            bytes(42) // u8 - State
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("42")
    }
}
