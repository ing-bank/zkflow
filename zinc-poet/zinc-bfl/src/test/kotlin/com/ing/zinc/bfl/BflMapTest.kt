package com.ing.zinc.bfl

import com.ing.zinc.bfl.ZincExecutor.createImports
import com.ing.zinc.bfl.ZincExecutor.generateCircuitBase
import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import com.ing.zinc.bfl.dsl.MapBuilder.Companion.map
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zkflow.util.bitSize
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

    @Test
    fun `toStructureTree should get size and structure correctly`() {
        val testSubject = map {
            capacity = 3
            keyType = BflPrimitive.U8
            valueType = BflPrimitive.U8
        }
        val actual = testSubject.toStructureTree()
        actual.bitSize shouldBe (24 * 2) + 32
        actual.toString() shouldBe """
            U8ToU8Map3: 80 bits (10 bytes)
            ├── size: 32 bits (4 bytes)
            │   └── u32: 32 bits (4 bytes)
            └── values: 48 bits (6 bytes)
                └── [U8ToU8MapEntry; 3]: 48 bits (6 bytes)
                    └── U8ToU8MapEntry: 16 bits (2 bytes)
                        ├── key: 8 bits (1 bytes)
                        │   └── u8: 8 bits (1 bytes)
                        └── value: 8 bits (1 bytes)
                            └── u8: 8 bits (1 bytes)
        """.trimIndent()
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
