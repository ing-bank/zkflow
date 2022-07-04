package com.ing.zkflow.zinc.poet.generate.structure

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class FindFieldChangesTest {
    @Test
    fun `findFieldChanges should detect an added field at the start`() {
        findFieldChanges(listOf(fieldB, fieldC), listOf(fieldA, fieldB, fieldC)) shouldBe listOf(AddedField(fieldA))
    }

    @Test
    fun `findFieldChanges should detect an added field in the middle`() {
        findFieldChanges(listOf(fieldA, fieldC), listOf(fieldA, fieldB, fieldC)) shouldBe listOf(AddedField(fieldB))
    }

    @Test
    fun `findFieldChanges should detect an added field at the end`() {
        findFieldChanges(listOf(fieldA, fieldB), listOf(fieldA, fieldB, fieldC)) shouldBe listOf(AddedField(fieldC))
    }

    @Test
    fun `findFieldChanges should detect a removed field at the start`() {
        findFieldChanges(listOf(fieldA, fieldB, fieldC), listOf(fieldB, fieldC)) shouldBe listOf(RemovedField(fieldA))
    }

    @Test
    fun `findFieldChanges should detect a removed field in the middle`() {
        findFieldChanges(listOf(fieldA, fieldB, fieldC), listOf(fieldA, fieldC)) shouldBe listOf(RemovedField(fieldB))
    }

    @Test
    fun `findFieldChanges should detect a removed field at the end`() {
        findFieldChanges(listOf(fieldA, fieldB, fieldC), listOf(fieldA, fieldB)) shouldBe listOf(RemovedField(fieldC))
    }

    @Test
    fun `findFieldChanges should detect a modified field at the start`() {
        findFieldChanges(listOf(fieldA, fieldB, fieldC), listOf(fieldD, fieldB, fieldC)) shouldBe
            listOf(ModifiedField(fieldA, fieldD))
    }

    @Test
    fun `findFieldChanges should detect a modified field in the middle`() {
        findFieldChanges(listOf(fieldA, fieldB, fieldC), listOf(fieldA, fieldD, fieldC)) shouldBe
            listOf(ModifiedField(fieldB, fieldD))
    }

    @Test
    fun `findFieldChanges should detect a modified field at the end`() {
        findFieldChanges(listOf(fieldA, fieldB, fieldC), listOf(fieldA, fieldB, fieldD)) shouldBe
            listOf(ModifiedField(fieldC, fieldD))
    }

    @Test
    fun `AddedField describe`() {
        AddedField(fieldA).describe() shouldBe "'a': new field with type 'kotlin.Byte'"
    }

    @Test
    fun `RemovedField describe`() {
        RemovedField(fieldA).describe() shouldBe "'a': deleted"
    }

    @Test
    fun `ModifiedField describe with different name`() {
        ModifiedField(fieldA, fieldA.copy(fieldName = "b")).describe() shouldBe "'a' renamed to 'b'"
    }

    @Test
    fun `ModifiedField describe with different size`() {
        ModifiedField(fieldE, fieldE.copy(fieldType = listOfBytes(12))).describe() shouldBe
            "'e': type changed from 'list(8) of kotlin.Byte' to 'list(12) of kotlin.Byte'"
    }

    @Test
    fun `ModifiedField describe with same type but different size`() {
        ModifiedField(fieldF, fieldF.copy(fieldType = classWithListOfBytes(12))).describe() shouldBe
            "'f': size of 'class fClass (id: null)' changed from 12 to 16 bytes"
    }

    companion object {
        private val bytePrimitive = ZkpStructurePrimitive("kotlin.Byte", Byte.SIZE_BYTES)
        private val intPrimitive = ZkpStructurePrimitive("kotlin.Int", Int.SIZE_BYTES)
        private val longPrimitive = ZkpStructurePrimitive("kotlin.Long", Long.SIZE_BYTES)
        private val booleanPrimitive = ZkpStructurePrimitive("kotlin.Boolean", 1)
        private val fieldA = ZkpStructureField("a", bytePrimitive)
        private val fieldB = ZkpStructureField("b", intPrimitive)
        private val fieldC = ZkpStructureField("c", longPrimitive)
        private val fieldD = ZkpStructureField("d", booleanPrimitive)
        private val fieldE = ZkpStructureField("e", listOfBytes(8))
        private val fieldF = ZkpStructureField("f", classWithListOfBytes(8))

        private fun listOfBytes(capacity: Int) = ZkpStructureList(capacity + Int.SIZE_BYTES, capacity, bytePrimitive)
        private fun classWithListOfBytes(capacity: Int): ZkpStructureClass {
            val fieldType = listOfBytes(capacity)
            return ZkpStructureClass(
                "fClass", null, null, fieldType.byteSize,
                listOf(
                    ZkpStructureField("fField", fieldType)
                )
            )
        }
    }
}
