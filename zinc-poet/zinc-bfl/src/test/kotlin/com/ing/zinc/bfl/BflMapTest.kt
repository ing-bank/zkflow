package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.createImports
import com.ing.zinc.bfl.ZincExecutor.generateCircuitBase
import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class BflMapTest {
    @Test
    fun `Map of enum to string should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(mapOfEnumToString)
        tempDir.generateWitness(SERIALIZED) {
            bytes(
                0, 0, 0, 2, // Map size: 2
                0, 0, 0, 0, // Things.NOTHING
                0, 0, 0, 1, // String size 1
                97, 0, // "a"
                0, 0, 0, 1, // Things.SOMETHING
                0, 0, 0, 2, // String size 2
                97, 98, // "ab"
            )
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe mapOf(
            Things.NOTHING to "a",
            Things.SOMETHING to "ab"
        ).asZincJsonMap(2)
    }

    @Test
    fun `Map method try_get should return some found item`(@TempDir tempDir: Path) {
        tempDir.generateTryGetCircuit(mapOfEnumToString, "Things::SOMETHING")
        tempDir.generateWitness {
            put(
                "map",
                mapOf(
                    Things.NOTHING to "a",
                    Things.SOMETHING to "ab"
                ).asZincJsonMap(2)
            )
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe zincJsonOptionOf("ab".asZincJsonString(2))
    }

    @Test
    fun `Map method try_get should return none for not found item`(@TempDir tempDir: Path) {
        tempDir.generateTryGetCircuit(mapOfEnumToString, "Things::ANYTHING")
        tempDir.generateWitness {
            put(
                "map",
                mapOf(
                    Things.NOTHING to "a",
                    Things.SOMETHING to "ab"
                ).asZincJsonMap(2)
            )
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe zincJsonOptionOf("".asZincJsonString(2), false)
    }

    private fun Path.generateTryGetCircuit(module: BflMap, key: String) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "map"; type = module.toZincId() }
                returnType = module.tryGetReturnType.toZincId()
                body = "map.try_get($key as ${module.keyType.id})"
            }
        }
    }
}
