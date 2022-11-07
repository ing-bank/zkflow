package com.ing.zkflow.serialization

import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.SizeAnnotation
import com.ing.zkflow.util.anyValue
import com.ing.zkflow.util.value
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

internal class StructureFromDescriptorsKtTest {
    @ParameterizedTest
    @CsvSource(
        "test.Foo,Foo",
        "test.Foo.Bar,Foo_Bar",
        "test,",
        "Test.foo,Test"
    )
    fun shorten(input: String, expected: String?) {
        input.shortenClassName() shouldBe (expected ?: "")
    }

    @Test
    fun `getFixedLength should return the size when SizeAnnotation present`() {
        serialDescriptorWithSizeAnnotation.fixedLength shouldBe 5
    }

    @Test
    fun `getFixedLength should return null when SizeAnnotation absent`() {
        serialDescriptorWithoutSizeAnnotation.fixedLength shouldBe null
    }

    @Test
    fun `toNodeDescriptor should shorten the serialName and calculate bitSize from descriptors byteSize`() {
        val descriptor = FixedLengthSerialDescriptor(serialDescriptorWithSizeAnnotation, 12)
        val actual = descriptor.toNodeDescriptor(null)
        actual.name shouldBe "Foo"
        actual.bitSize shouldBe 12 * Byte.SIZE_BITS
    }

    @Test
    fun `toNodeDescriptor should include capacity in the name when present`() {
        val descriptor = FixedLengthSerialDescriptor(serialDescriptorWithSizeAnnotation, 12)
        val actual = descriptor.toNodeDescriptor(3)
        actual.name shouldBe "Foo (capacity: 3)"
        actual.bitSize shouldBe 12 * Byte.SIZE_BITS
    }

    @Test
    fun `toNodeDescriptor should apply capacity to the bitSize for ArrayList`() {
        val descriptor = FixedLengthSerialDescriptor(serialDescriptorForArrayList, 12)
        val actual = descriptor.toNodeDescriptor(3)
        actual.name shouldBe "ArrayList (capacity: 3)"
        actual.bitSize shouldBe 12 * Byte.SIZE_BITS * 3
    }

    @Test
    fun `toStructureTree should work without capacity`() {
        val descriptor = FixedLengthSerialDescriptor(serialDescriptorWithoutSizeAnnotation, 12)
        val actual = descriptor.toStructureTree(null)
        actual.toString() shouldBe "Bar: 96 bits (12 bytes)"
    }

    @Test
    fun `toStructureTree should use capacity from parentCapacity parameter`() {
        val descriptor = FixedLengthSerialDescriptor(serialDescriptorWithoutSizeAnnotation, 12)
        val actual = descriptor.toStructureTree(3)
        actual.toString() shouldBe "Bar (capacity: 3): 96 bits (12 bytes)"
    }

    @Test
    fun `toStructureTree should use capacity from SizeAnnotation`() {
        val descriptor = FixedLengthSerialDescriptor(serialDescriptorWithSizeAnnotation, 12)
        val actual = descriptor.toStructureTree(null)
        actual.toString() shouldBe "Foo (capacity: 5): 96 bits (12 bytes)"
    }

    @Test
    fun `toStructureTree should raise an exception when both parentCapacity parameter and SizeAnnotation are found`() {
        shouldThrow<IllegalArgumentException> {
            val descriptor = FixedLengthSerialDescriptor(serialDescriptorWithSizeAnnotation, 12)
            descriptor.toStructureTree(3)
        }.message shouldBe "Only 1 of parentCapacity or SizeAnnotation is allowed for test.Foo"
    }

    @Test
    fun `toStructureTree should exclude nodes with elementName 0`() {
        val serialDescriptor = buildClassSerialDescriptor("Test") {
            element("0", IntSerializer.descriptor)
        }
        val descriptor = serialDescriptor.toFixedLengthSerialDescriptorOrThrow()
        val actual = descriptor.toStructureTree(null)
        // Show that descriptor contains element with name "0"
        descriptor.containsElementWithName("0") shouldBe true
        // Assert that actual does not contain a node with "0"
        actual.anyValue { it.name.startsWith("0") } shouldBe false
    }

    @Test
    fun `toStructureTree should pass capacity to nodes with elementName values`() {
        val serialDescriptor = buildClassSerialDescriptor("Test") {
            element("values", IntSerializer.descriptor)
        }
        val descriptor = serialDescriptor.toFixedLengthSerialDescriptorOrThrow()
        val actual = descriptor.toStructureTree(3)
        actual.anyValue { it.name.startsWith("Int (capacity: 3)") } shouldBe true
    }

    @ParameterizedTest
    @MethodSource("primitivesToTree")
    fun `toTree should successfully convert primitives`(descriptor: SerialDescriptor, nodeDescription: String) {
        toTree(descriptor).value.toString() shouldBe nodeDescription
    }

    @Test
    fun `toTree should work for lists`() {
        val actual = toTree(FixedLengthListSerializer(8, IntSerializer).descriptor)
        actual.toString() shouldBe """
            FixedLengthListSerializer (capacity: 8): 288 bits (36 bytes)
            ├── size: 32 bits (4 bytes)
            │   └── i32: 32 bits (4 bytes)
            └── values: 256 bits (32 bytes)
                └── ArrayList (capacity: 8): 256 bits (32 bytes)
                    └── Int: 32 bits (4 bytes)
        """.trimIndent()
    }

    companion object {
        @JvmStatic
        fun primitivesToTree(): List<Arguments> = listOf(
            Arguments.of(Byte.serializer().descriptor, "i8: 8 bits (1 bytes)"),
            Arguments.of(Short.serializer().descriptor, "i16: 16 bits (2 bytes)"),
            Arguments.of(Int.serializer().descriptor, "i32: 32 bits (4 bytes)"),
            Arguments.of(Long.serializer().descriptor, "i64: 64 bits (8 bytes)"),
            Arguments.of(Boolean.serializer().descriptor, "bool: 8 bits (1 bytes)"),
        )

        private val serialDescriptorWithSizeAnnotation = buildClassSerialDescriptor("test.Foo") {
            annotations = listOf(SizeAnnotation(5))
        }

        private val serialDescriptorWithoutSizeAnnotation = buildClassSerialDescriptor("test.Bar") {
        }

        private val serialDescriptorForArrayList = buildClassSerialDescriptor("something.something.ArrayList") {
            annotations = listOf(SizeAnnotation(5))
        }

        private fun SerialDescriptor.containsElementWithName(elementName: String): Boolean {
            return (0 until elementsCount).fold(false) { acc: Boolean, index: Int ->
                acc || getElementName(index) == elementName || getElementDescriptor(index).containsElementWithName(elementName)
            }
        }
    }
}
