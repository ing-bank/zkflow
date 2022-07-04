package com.ing.zkflow.zinc.poet.generate.structure

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ZkpStructureComparisonTest {
    @Test
    fun `ZkpStructureComparison should allow name changes for classes with serializationId`() {
        val actual = compare(
            listOf(emptyStateA),
            listOf(emptyStateA.copy(serialName = "example.SomeOtherA"))
        )
        actual shouldBe emptyList()
    }

    @Test
    fun `ZkpStructureComparison should allow same classes without serializationId`() {
        val actual = compare(
            listOf(emptyStateA.copy(serializationId = null)),
            listOf(emptyStateA.copy(serializationId = null)),
        )
        actual shouldBe emptyList()
    }

    @Test
    fun `ZkpStructureComparison cannot detect class name changes for classes without serializationId`() {
        val actual = compare(
            listOf(emptyStateA.copy(serializationId = null)),
            listOf(emptyStateA.copy(serializationId = null, serialName = "example.SomeOtherA"))
        )
        actual shouldBe listOf(RemovedClasses(listOf(emptyStateA.copy(serializationId = null))))
    }

    @Test
    fun `ZkpStructureComparison should detect removed classes`() {
        val actual = compare(
            listOf(emptyStateA, emptyStateB),
            listOf(emptyStateB),
        )
        actual shouldContainInOrder listOf(RemovedClasses(listOf(emptyStateA)))
    }

    @Test
    fun `ZkpStructureComparison should allow added classes`() {
        val actual = compare(
            listOf(emptyStateA),
            listOf(emptyStateA, emptyStateB),
        )
        actual shouldBe emptyList()
    }

    @Test
    fun `ZkpStructureComparison should detect classes with changed byteSize`() {
        val actual = compare(
            listOf(emptyStateA),
            listOf(stateAWithIntegerAndByteField),
        )
        actual shouldContainInOrder listOf(ModifiedClasses(listOf(Pair(emptyStateA, stateAWithIntegerAndByteField))))
    }

    @Test
    fun `ZkpStructureComparison should detect classes with different number of fields`() {
        val actual = compare(
            listOf(emptyStateA),
            listOf(stateAWithEmptyField),
        )
        actual shouldContainInOrder listOf(ModifiedClasses(listOf(Pair(emptyStateA, stateAWithEmptyField))))
    }

    @Test
    fun `ZkpStructureComparison should detect classes with changed fields`() {
        val actual = compare(
            listOf(stateAWithByteAndIntegerField),
            listOf(stateAWithIntegerAndByteField),
        )
        actual shouldContainInOrder listOf(
            ModifiedClasses(
                listOf(
                    Pair(stateAWithByteAndIntegerField, stateAWithIntegerAndByteField)
                )
            )
        )
    }

    @Test
    fun `ZkpStructureComparison should detect classes with changed list sizes`() {
        val actual = compare(
            listOf(stateAWithList16),
            listOf(stateAWithList20),
        )
        actual shouldContainInOrder listOf(
            ModifiedClasses(listOf(Pair(stateAWithList16, stateAWithList20)))
        )
    }

    companion object {
        private val aByte = ZkpStructurePrimitive("kotlin.Byte", Byte.SIZE_BYTES)
        private val anInt = ZkpStructurePrimitive("kotlin.Int", Int.SIZE_BYTES)

        private val aByteField = ZkpStructureField("byte", aByte)
        private val anIntField = ZkpStructureField("integer", anInt)

        private val emptyStateA = ZkpStructureClass("example.A", null, 1, 0, emptyList())
        private val emptyStateB = ZkpStructureClass("example.B", null, 2, 0, emptyList())

        private val stateAWithEmptyField = ZkpStructureClass(
            "example.A", null, 1, 0,
            listOf(ZkpStructureField("empty", emptyStateB))
        )
        private val stateAWithIntegerAndByteField = ZkpStructureClass(
            "example.A", null, 1, 5,
            listOf(anIntField, aByteField)
        )
        private val stateAWithByteAndIntegerField = ZkpStructureClass(
            "example.A", null, 1, 5,
            listOf(aByteField, anIntField)
        )
        private val list16OfByte = ZkpStructureList(20, 16, aByte)
        private val list20OfByte = ZkpStructureList(24, 20, aByte)
        private val stateAWithList16 = ZkpStructureClass(
            "example.A", null, null, 20,
            listOf(ZkpStructureField("list", list16OfByte))
        )
        private val stateAWithList20 = ZkpStructureClass(
            "example.A", null, null, 24,
            listOf(ZkpStructureField("list", list20OfByte))
        )
    }
}
