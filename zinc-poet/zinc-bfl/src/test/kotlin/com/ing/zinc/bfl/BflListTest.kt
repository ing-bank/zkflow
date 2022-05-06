package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflList.Companion.ELEMENT
import com.ing.zinc.bfl.ZincExecutor.createImports
import com.ing.zinc.bfl.ZincExecutor.generateCircuitBase
import com.ing.zinc.bfl.ZincExecutor.generateDeserializeCircuit
import com.ing.zinc.bfl.ZincExecutor.generateWitness
import com.ing.zinc.bfl.ZincExecutor.runCommandAndLogTime
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.util.bitSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class BflListTest {
    @Test
    fun `List of Units should deserialize correctly`(@TempDir tempDir: Path) {
        val listOfUnits = list {
            elementType = BflUnit
            capacity = 2
        }
        tempDir.generateDeserializeCircuit(listOfUnits)
        tempDir.generateWitness(SERIALIZED) {
            bytes(0, 0, 0, 1)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe listOf(JsonPrimitive("unit")).asZincJsonObjectList(2, JsonPrimitive("unit"))
    }

    @Test
    fun `List of Bools deserialize method should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(listOfBools)
        tempDir.generateWitness(SERIALIZED) {
            bytes(0, 0, 0, 1) // size: 1
            bits(
                1, // values[0]: true
                0, // values[1]: false
            )
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe listOf(true).asZincJsonBooleanList(2)
    }

    @Test
    fun `List of Enums deserialize method should deserialize correctly and bogus should be skipped`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(listOfEnums)
        tempDir.generateWitness(SERIALIZED) {
            bytes(
                0, 0, 0, 1, // size: 1
                0, 0, 0, 1, // values[0]: SOMETHING
                255, 255, 255, 255, // values[1]: bogus content
            )
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe listOf(1).asZincJsonNumberList(2)
    }

    @Test
    fun `List of arrays deserialize method should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(listOfArraysOfU8)
        tempDir.generateWitness(SERIALIZED) {
            bytes(
                0, 0, 0, 1,
                5, 8,
                13, 21
            )
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe testDataListOfArrays
    }

    @Test
    fun `List of structs deserialize method should deserialize correctly`(@TempDir tempDir: Path) {
        tempDir.generateDeserializeCircuit(listOfStructWithStructField)
        tempDir.generateWitness(SERIALIZED) {
            bytes(0, 0, 0, 1)
            bytes(0, 0, 0, 1).bits(1)
            bytes(0, 0, 0, 0).bits(0)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe testDataListOfStructWithStructs
    }

    @Test
    fun `List of structs get method should get a value at index successfully`(@TempDir tempDir: Path) {
        tempDir.generateGetCircuit(listOfStructWithStructField, 0)
        tempDir.generateWitness { put("list", testDataListOfStructWithStructs) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe structStructJson(true, 1)
    }

    @Test
    fun `List of structs get method should not get a value when index out of bounds`(@TempDir tempDir: Path) {
        tempDir.generateGetCircuit(listOfStructWithStructField, 1)
        tempDir.generateWitness { put("list", testDataListOfStructWithStructs) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldContain "Index out of bounds"
        stdout shouldBe ""
    }

    @Test
    fun `List of structs contains method should detect whether the list contains a value`(@TempDir tempDir: Path) {
        tempDir.generateContainsCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put("list", testDataListOfStructWithStructs)
            put(ELEMENT, structStructJson(true, 1))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `List of structs contains method should detect whether the list does not contain a value`(@TempDir tempDir: Path) {
        tempDir.generateContainsCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put("list", testDataListOfStructWithStructs)
            put(ELEMENT, structStructJson(true, 13))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `List of structs is_subset_of method should return true for same list`(@TempDir tempDir: Path) {
        tempDir.generateIsSubsetCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put("this", testDataListOfStructWithStructs)
            put("that", testDataListOfStructWithStructs)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `List of structs is_subset_of method should return true for empty list`(@TempDir tempDir: Path) {
        tempDir.generateIsSubsetCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put("this", testDataEmptyListOfStructWithStructs)
            put("that", testDataListOfStructWithStructs)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `List of structs is_subset_of method should return true for filled list`(@TempDir tempDir: Path) {
        tempDir.generateIsSubsetCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put("this", testDataListOfStructWithStructs)
            put("that", testDataLargerListOfStructWithStructs)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `List of structs is_subset_of method should return false for larger list`(@TempDir tempDir: Path) {
        tempDir.generateIsSubsetCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put("this", testDataLargerListOfStructWithStructs)
            put("that", testDataListOfStructWithStructs)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `List of structs extract_baz_foo method should correctly extract baz_foo fields`(@TempDir tempDir: Path) {
        tempDir.generateExtractFieldCircuit(listOfStructWithStructField, "baz_foo", BflPrimitive.U32)
        tempDir.generateWitness { put("this", testDataLargerListOfStructWithStructs) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe listOf(1, 13).asZincJsonNumberList(2, 0)
    }

    @Test
    fun `List of structs extract_baz_bar method should correctly extract baz_bar fields`(@TempDir tempDir: Path) {
        tempDir.generateExtractFieldCircuit(listOfStructWithStructField, "baz_bar", BflPrimitive.Bool)
        tempDir.generateWitness { put("this", testDataListOfStructWithStructs) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe listOf(true).asZincJsonBooleanList(2, false)
    }

    @Test
    fun `List of structs extract_baz method should correctly extract baz fields`(@TempDir tempDir: Path) {
        tempDir.generateExtractFieldCircuit(listOfStructWithStructField, "baz", structWithPrimitiveFields)
        tempDir.generateWitness { put("this", testDataLargerListOfStructWithStructs) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe testDataLargerListOfStructs
    }

    @Test
    fun `List of structs add method should append an element to a list that is not full`(@TempDir tempDir: Path) {
        tempDir.generateAddCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put(ELEMENT, structStructJson(true, 1))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe testDataListOfStructWithStructs
    }

    @Test
    fun `List of structs is_distinct method should return false for list with duplicates`(@TempDir tempDir: Path) {
        tempDir.generateIsDistinctCircuit(listOfStructWithStructField)
        tempDir.generateWitness { put("this", testDataDuplicateListOfStructWithStructs) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `List of structs is_distinct method should return true for list without duplicates`(@TempDir tempDir: Path) {
        tempDir.generateIsDistinctCircuit(listOfStructWithStructField)
        tempDir.generateWitness { put("this", testDataLargerListOfStructWithStructs) }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `List of structs all_equals method should return true for a list with duplicates`(@TempDir tempDir: Path) {
        tempDir.generateAllEqualsCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put("list", testDataDuplicateListOfStructWithStructs)
            put(ELEMENT, structStructJson(true, 1))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(true)
    }

    @Test
    fun `List of structs all_equals method should return false for a list different items`(@TempDir tempDir: Path) {
        tempDir.generateAllEqualsCircuit(listOfStructWithStructField)
        tempDir.generateWitness {
            put("list", testDataLargerListOfStructWithStructs)
            put(ELEMENT, structStructJson(true, 1))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive(false)
    }

    @Test
    fun `List of structs index_of_single_by_baz_foo method should correctly extract baz_foo fields`(@TempDir tempDir: Path) {
        tempDir.generateIndexOfSingleByFieldCircuit(listOfStructWithStructField, "baz_foo", BflPrimitive.U32)
        tempDir.generateWitness {
            put("this", testDataLargerListOfStructWithStructs)
            put("by", "13")
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("1")
    }

    @Test
    fun `List of structs index_of_single_by_baz_bar method should correctly extract baz_bar fields`(@TempDir tempDir: Path) {
        tempDir.generateIndexOfSingleByFieldCircuit(listOfStructWithStructField, "baz_bar", BflPrimitive.Bool)
        tempDir.generateWitness {
            put("this", testDataListOfStructWithStructs)
            put("by", true)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("0")
    }

    @Test
    fun `List of structs index_of_single_by_baz method should correctly extract baz fields`(@TempDir tempDir: Path) {
        tempDir.generateIndexOfSingleByFieldCircuit(listOfStructWithStructField, "baz", structWithPrimitiveFields)
        tempDir.generateWitness {
            put("this", testDataLargerListOfStructWithStructs)
            put("by", primitiveStructJson(true, 13))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe JsonPrimitive("1")
    }

    @Test
    fun `List of structs index_of_single_by_baz_foo method should error when duplicate matches found`(@TempDir tempDir: Path) {
        tempDir.generateIndexOfSingleByFieldCircuit(listOfStructWithStructField, "baz_foo", BflPrimitive.U32)
        tempDir.generateWitness {
            put("this", testDataDuplicateListOfStructWithStructs)
            put("by", "1")
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldContain "Multiple matches found for field baz.foo"
        stdout shouldBe ""
    }

    @Test
    fun `List of structs index_of_single_by_baz_foo method should error when no matches found`(@TempDir tempDir: Path) {
        tempDir.generateIndexOfSingleByFieldCircuit(listOfStructWithStructField, "baz_foo", BflPrimitive.U32)
        tempDir.generateWitness {
            put("this", testDataDuplicateListOfStructWithStructs)
            put("by", "2")
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldContain "No match found for field baz.foo"
        stdout shouldBe ""
    }

    @Test
    fun `List of structs single_by_baz_foo method should correctly extract baz_foo fields`(@TempDir tempDir: Path) {
        tempDir.generateSingleByFieldCircuit(listOfStructWithStructField, "baz_foo", BflPrimitive.U32)
        tempDir.generateWitness {
            put("this", testDataLargerListOfStructWithStructs)
            put("by", "13")
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe structStructJson(true, 13)
    }

    @Test
    fun `List of structs single_by_baz_bar method should correctly extract baz_bar fields`(@TempDir tempDir: Path) {
        tempDir.generateSingleByFieldCircuit(listOfStructWithStructField, "baz_bar", BflPrimitive.Bool)
        tempDir.generateWitness {
            put("this", testDataListOfStructWithStructs)
            put("by", true)
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe structStructJson(true, 1)
    }

    @Test
    fun `List of structs single_by_baz method should correctly extract baz fields`(@TempDir tempDir: Path) {
        tempDir.generateSingleByFieldCircuit(listOfStructWithStructField, "baz", structWithPrimitiveFields)
        tempDir.generateWitness {
            put("this", testDataLargerListOfStructWithStructs)
            put("by", primitiveStructJson(true, 13))
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldBe ""
        stdout.parseJson() shouldBe structStructJson(true, 13)
    }

    @Test
    fun `List of structs single_by_baz_foo method should error when duplicate matches found`(@TempDir tempDir: Path) {
        tempDir.generateSingleByFieldCircuit(listOfStructWithStructField, "baz_foo", BflPrimitive.U32)
        tempDir.generateWitness {
            put("this", testDataDuplicateListOfStructWithStructs)
            put("by", "1")
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldContain "Multiple matches found for field baz.foo"
        stdout shouldBe ""
    }

    @Test
    fun `List of structs single_by_baz_foo method should error when no matches found`(@TempDir tempDir: Path) {
        tempDir.generateSingleByFieldCircuit(listOfStructWithStructField, "baz_foo", BflPrimitive.U32)
        tempDir.generateWitness {
            put("this", testDataDuplicateListOfStructWithStructs)
            put("by", "2")
        }

        val (stdout, stderr) = tempDir.runCommandAndLogTime("zargo run")

        stderr shouldContain "No match found for field baz.foo"
        stdout shouldBe ""
    }

    @Test
    fun `toStructureTree should get size and structure correctly`() {
        val testSubject = list {
            capacity = 3
            elementType = BflPrimitive.U8
        }
        val actual = testSubject.toStructureTree()
        actual.bitSize shouldBe 24 + 32
        actual.toString() shouldBe """
            U8List3: 56 bits (7 bytes)
            ├── size: 32 bits (4 bytes)
            │   └── u32: 32 bits (4 bytes)
            └── values: 24 bits (3 bytes)
                └── [u8; 3]: 24 bits (3 bytes)
                    └── u8: 8 bits (1 bytes)
        """.trimIndent()
    }

    private fun Path.generateGetCircuit(module: BflList, index: Int) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "list"; type = module.toZincId() }
                returnType = module.elementType.toZincId()
                body = "list.get($index as ${module.sizeType.id})"
            }
        }
    }

    private fun Path.generateContainsCircuit(module: BflList) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "list"; type = module.toZincId() }
                parameter { name = ELEMENT; type = module.elementType.toZincId() }
                returnType = ZincPrimitive.Bool
                body = "list.contains(element)"
            }
        }
    }

    private fun Path.generateIsSubsetCircuit(module: BflList) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "this"; type = module.toZincId() }
                parameter { name = "that"; type = module.toZincId() }
                returnType = ZincPrimitive.Bool
                body = "this.is_subset_of(that)"
            }
        }
    }

    private fun Path.generateExtractFieldCircuit(
        module: BflList,
        extractedField: String,
        extractedElementType: BflType
    ) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "this"; type = module.toZincId() }
                returnType = BflList(module.capacity, extractedElementType).toZincId()
                body = "this.extract_$extractedField()"
            }
        }
    }

    private fun Path.generateAddCircuit(module: BflList) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = ELEMENT; type = module.elementType.toZincId() }
                returnType = module.toZincId()
                body = """
                    let mut out = ${module.id}::empty();
                    out.add(element)
                """.trimIndent()
            }
        }
    }

    private fun Path.generateIsDistinctCircuit(module: BflList) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "this"; type = module.toZincId() }
                returnType = ZincPrimitive.Bool
                body = "this.is_distinct()"
            }
        }
    }

    private fun Path.generateAllEqualsCircuit(module: BflList) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "list"; type = module.toZincId() }
                parameter { name = ELEMENT; type = module.elementType.toZincId() }
                returnType = ZincPrimitive.Bool
                body = "list.all_equals(element)"
            }
        }
    }

    private fun Path.generateIndexOfSingleByFieldCircuit(
        module: BflList,
        byField: String,
        byElementType: BflType
    ) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "this"; type = module.toZincId() }
                parameter { name = "by"; type = byElementType.toZincId() }
                returnType = module.sizeType.toZincId()
                body = "this.index_of_single_by_$byField(by)"
            }
        }
    }

    private fun Path.generateSingleByFieldCircuit(
        module: BflList,
        byField: String,
        byElementType: BflType
    ) {
        generateCircuitBase(module)
        // generate src/main.zn
        zincSourceFile("main.zn") {
            module.allModules {
                createImports(this)
            }
            function {
                name = "main"
                parameter { name = "this"; type = module.toZincId() }
                parameter { name = "by"; type = byElementType.toZincId() }
                returnType = module.elementType.toZincId()
                body = "this.single_by_$byField(by)"
            }
        }
    }
}
