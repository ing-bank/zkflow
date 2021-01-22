package com.ing.zknotary.ksp.generator

import com.ing.zknotary.annotations.SizedString
import com.ing.zknotary.ksp.generator.helpers.KSPCompilationHelper
import com.ing.zknotary.ksp.generator.helpers.TestMatchers.shouldBeAlignedWith
import com.ing.zknotary.ksp.generator.helpers.constructorFrom
import com.ing.zknotary.ksp.generator.helpers.get
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Test functionality for compound types and collections thereof.
 *
 * Compound types as defined in TypeDescriptor.compoundTypes.
 */
class CompoundTypesTest {
    @Test
    fun `class with strings must be sizeable`() {
        val loader = KSPCompilationHelper.buildSizedVersion(
            KSPCompilationHelper.SimpleSource(
                "StringState",
                "@Sized class StringState(val string: @FixToLength(10) String)"
            )
        )

        val sourceClass = loader.load("StringState")
        val targetClass = loader.load("StringStateSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClass

        // Construction alignment.
        val source = sourceClass.constructors.single().call("a")
        val target = targetClass.constructorFrom(sourceClass).call(source)

        assert(target["string"] is SizedString)
        target["string"]["string"] shouldBe "a" + List(9) { '0' }.joinToString(separator = "")
        target["string"]["originalLength"] shouldBe 1
    }

    @Test
    fun `class with compound types must be sizeable`() {
        val loader = KSPCompilationHelper.buildSizedVersion(
            KSPCompilationHelper.SimpleSource(
                "SimpleCompoundState",
                "@Sized class SimpleCompoundState(val pair: Pair<Int, Int>)"
            )
        )

        val sourceClass = loader.load("SimpleCompoundState")
        val targetClass = loader.load("SimpleCompoundStateSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClass

        // Construction alignment.
        val source = sourceClass.constructors.single().call(Pair(19, 84))
        val target = targetClass.constructorFrom(sourceClass).call(source)

        assert(target["pair"] is Pair<*, *>)

        target["pair"]["first"] shouldBe 19
        target["pair"]["second"] shouldBe 84
    }

    @Test
    fun `class with complex compound types must be sizeable`() {
        val loader = KSPCompilationHelper.buildSizedVersion(
            KSPCompilationHelper.SimpleSource(
                "ComplexCompoundState",
                "@Sized class ComplexCompoundState(val triple: Triple<Int, Int, Int>)"
            )
        )

        val sourceClass = loader.load("ComplexCompoundState")
        val targetClass = loader.load("ComplexCompoundStateSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClass

        // Construction alignment.
        val source = sourceClass.constructors.single().call(Triple(5, 19, 84))
        val target = targetClass.constructorFrom(sourceClass).call(source)

        assert(target["triple"] is Triple<*, *, *>)

        target["triple"]["first"] shouldBe 5
        target["triple"]["second"] shouldBe 19
        target["triple"]["third"] shouldBe 84
    }

    @Test
    fun `class with deep compound types must be sizeable`() {
        val loader = KSPCompilationHelper.buildSizedVersion(
            KSPCompilationHelper.SimpleSource(
                "DeepCompoundState",
                """
                @Sized
                class DeepCompoundState(
                    val deep:
                    @FixToLength(10) List<
                        @FixToLength(9) List<
                            Pair<
                                Int,
                                @FixToLength(8) List<Int>>>>
                )
                """.trimIndent()
            )
        )

        val sourceClass = loader.load("DeepCompoundState")
        val targetClass = loader.load("DeepCompoundStateSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClass

        // Construction alignment.
        val source = sourceClass.constructors.single().call(listOf(listOf(Pair(19, listOf(84)))))
        val target = targetClass.constructorFrom(sourceClass).call(source)

        val sourceL1 = source["deep"]
        val targetL1 = target["deep"]
        targetL1["size"] shouldBe 10
        targetL1["originalSize"] shouldBe sourceL1["size"]

        val sourceL2 = sourceL1["0"]
        val targetL2 = targetL1["0"]
        targetL2["size"] shouldBe 9
        targetL2["originalSize"] shouldBe sourceL2["size"]
        assert(targetL2["0"] is Pair<*, *>)

        val sourceL3 = sourceL2["0"]["second"]
        val targetL3 = targetL2["0"]["second"]
        targetL3["size"] shouldBe 8
        targetL3["originalSize"] shouldBe sourceL3["size"]
    }
}
