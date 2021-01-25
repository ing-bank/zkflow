package com.ing.zknotary.ksp.generator

import com.ing.zknotary.ksp.generator.helpers.KSPCompilationHelper
import com.ing.zknotary.ksp.generator.helpers.TestMatchers.shouldBeAlignedWith
import com.ing.zknotary.ksp.generator.helpers.constructorFrom
import com.ing.zknotary.ksp.generator.helpers.get
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Test functionality for primitive types and collections thereof.
 *
 * Primitive types as defined in TypeDescriptor.primitiveTypes.
 */
class PrimitiveTypesTest {
    @Test
    fun `class with primitive types must be sizeable`() {
        fun <T : Any> testWrapper(primitive: T) {
            val loader = KSPCompilationHelper.buildSizedVersion(
                KSPCompilationHelper.SimpleSource(
                    "SimpleState",
                    """
                    @Sized
                    class SimpleState(val primitive: ${primitive::class.simpleName!!})
                    """.trimIndent()
                )
            )

            val sourceClass = loader.load("SimpleState")
            val targetClass = loader.load("SimpleStateSized")

            // Structural integrity and alignment.
            targetClass shouldBeAlignedWith sourceClass

            // Construction alignment.

            val source = sourceClass.constructors.single().call(primitive)
            val target = targetClass.constructorFrom(sourceClass).call(source)

            target["primitive"] shouldBe source["primitive"]
            assert(target["primitive"]::class == primitive::class)
        }

        testWrapper(100.toByte())
        testWrapper(100.toShort())
        testWrapper(100.toInt())
        testWrapper(100.toLong())
        testWrapper(false)
        testWrapper('a')
    }

    @Test
    fun `class with list of primitive types must be sizeable`() {
        fun <T : Any> testWrapper(primitive: T) {
            val loader = KSPCompilationHelper.buildSizedVersion(
                KSPCompilationHelper.SimpleSource(
                    "ListState",
                    """
                    @Sized
                    class ListState(
                        val shallow: @FixToLength(7) List<${primitive::class.simpleName!!}>,
                        val deep: @FixToLength(5) List<@FixToLength(2) List<${primitive::class.simpleName!!}>>
                    )
                    """.trimIndent()
                )
            )

            val sourceClass = loader.load("ListState")
            val targetClass = loader.load("ListStateSized")

            // Structural integrity and alignment.
            targetClass shouldBeAlignedWith sourceClass

            // Construction alignment.
            val source = sourceClass.constructors.single()
                .call(List(3) { primitive }, List(2) { List(1) { primitive } })
            val target = targetClass.constructorFrom(sourceClass).call(source)

            target["shallow"]["size"] shouldBe 7
            target["shallow"]["originalSize"] shouldBe source["shallow"]["size"]
            assert(target["shallow"]["0"]::class == primitive::class)

            target["deep"]["size"] shouldBe 5
            target["deep"]["originalSize"] shouldBe source["deep"]["size"]

            target["deep"]["0"]["size"] shouldBe 2
            target["deep"]["0"]["originalSize"] shouldBe source["deep"]["0"]["size"]
            assert(target["deep"]["0"]["0"]::class == primitive::class)
        }

        testWrapper(100.toByte())
        testWrapper(100.toShort())
        testWrapper(100.toInt())
        testWrapper(100.toLong())
        testWrapper(false)
        testWrapper('a')
    }

    @Test
    fun `class with array of primitive types must be sizeable`() {
        fun <T : Any> testWrapper(primitive: T) {
            val arrayLength = 2

            val (specializedName, generic, specialized) = when (primitive) {
                is Byte -> Triple(
                    "ByteArray",
                    Array<Byte>(arrayLength) { primitive },
                    ByteArray(arrayLength) { primitive }
                )
                is Short -> Triple(
                    "ShortArray",
                    Array<Short>(arrayLength) { primitive },
                    ShortArray(arrayLength) { primitive }
                )
                is Int -> Triple(
                    "IntArray",
                    Array<Int>(arrayLength) { primitive },
                    IntArray(arrayLength) { primitive }
                )
                is Long -> Triple(
                    "LongArray",
                    Array<Long>(arrayLength) { primitive },
                    LongArray(arrayLength) { primitive }
                )
                is Boolean -> Triple(
                    "BooleanArray",
                    Array<Boolean>(arrayLength) { primitive },
                    BooleanArray(arrayLength) { primitive }
                )
                is Char -> Triple(
                    "CharArray",
                    Array<Char>(arrayLength) { primitive },
                    CharArray(arrayLength) { primitive }
                )
                else -> error("Unsupported type ${primitive::class.simpleName}")
            }

            val loader = KSPCompilationHelper.buildSizedVersion(
                KSPCompilationHelper.SimpleSource(
                    "ArrayState",
                    """
                    @Sized
                    class ArrayState(
                        val generic: @FixToLength(7) Array<${primitive::class.simpleName!!}>,
                        val specialized: @FixToLength(7) $specializedName
                    )
                    """.trimIndent()
                )
            )

            val sourceClass = loader.load("ArrayState")
            val targetClass = loader.load("ArrayStateSized")

            // Structural integrity and alignment.
            targetClass shouldBeAlignedWith sourceClass

            // Construction alignment.
            val source = sourceClass.constructors.single()
                .call(generic, specialized)
            val target = targetClass.constructorFrom(sourceClass).call(source)

            target["generic"]["size"] shouldBe 7
            target["generic"]["originalSize"] shouldBe arrayLength
            assert(target["generic"]["0"]::class == primitive::class)

            target["specialized"]["size"] shouldBe 7
            target["specialized"]["originalSize"] shouldBe arrayLength
            assert(target["specialized"]["0"]::class == primitive::class)
        }

        testWrapper(100.toByte())
        testWrapper(100.toShort())
        testWrapper(100.toInt())
        testWrapper(100.toLong())
        testWrapper(false)
        testWrapper('a')
    }
}
