package com.ing.zknotary.ksp.generator

import com.ing.zknotary.ksp.SizedProcessor
import com.squareup.kotlinpoet.asTypeName
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessors
import io.kotest.matchers.reflection.shouldHaveMemberProperty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class SizedTest {
    @Test
    fun `class with simple types must be sizeable`() {
        val loader = buildSizedVersion(
            SimpleSource(
                "SimpleState",
                """
                @Sized
                class SimpleState(val simple: Int)
                """.trimIndent()
            )
        )

        val sourceClass = loader.load("SimpleState")
        val targetClass = loader.load("SimpleStateSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClass

        // Construction alignment.
        val source = sourceClass.constructors.single().call(100)
        val target = targetClass.constructorFrom(sourceClass).call(source)

        target["simple"] shouldBe source["simple"]
    }

    @Test
    fun `class with list of ints must be sizeable`() {
        val loader = buildSizedVersion(
            SimpleSource(
                "ListState",
                """
                @Sized
                class ListState(
                    val shallow: @FixToLength(7) List<Int>,
                    val deep: @FixToLength(5) List<@FixToLength(2) List<Int>>
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
            .call(List(3) { 2 }, List(2) { List(1) { 1 } })
        val target = targetClass.constructorFrom(sourceClass).call(source)

        target["shallow"]["list"]["size"] shouldBe 7
        target["shallow"]["originalSize"] shouldBe source["shallow"]["size"]

        target["deep"]["list"]["size"] shouldBe 5
        target["deep"]["originalSize"] shouldBe source["deep"]["size"]

        target["deep"]["list"]["0"]["list"]["size"] shouldBe 2
        target["deep"]["list"]["0"]["originalSize"] shouldBe source["deep"]["0"]["size"]
    }

    @Test
    fun `class with compound types must be sizeable`() {
        val loader = buildSizedVersion(
            SimpleSource(
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
        val loader = buildSizedVersion(
            SimpleSource(
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
        val loader = buildSizedVersion(
            SimpleSource(
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
        targetL1["list"]["size"] shouldBe 10
        targetL1["originalSize"] shouldBe sourceL1["size"]

        val sourceL2 = sourceL1["0"]
        val targetL2 = targetL1["list"]["0"]
        targetL2["list"]["size"] shouldBe 9
        targetL2["originalSize"] shouldBe sourceL2["size"]
        assert(targetL2["list"]["0"] is Pair<*, *>)

        val sourceL3 = sourceL2["0"]["second"]
        val targetL3 = targetL2["list"]["0"]["second"]
        targetL3["list"]["size"] shouldBe 8
        targetL3["originalSize"] shouldBe sourceL3["size"]
    }

    @Test
    fun `class with custom sizeable types must be sizeable`() {
        val loader = buildSizedVersion(
            SimpleSource(
                "StateL0",
                "@Sized class StateL0(val simple: @FixToLength(4) List<Int>)"
            ),
            SimpleSource(
                "StateL1",
                "@Sized class StateL1(val complex: @FixToLength(5) List<StateL0>)"
            )
        )

        val sourceClassL0 = loader.load("StateL0")
        val sourceClassL1 = loader.load("StateL1")
        val targetClass = loader.load("StateL1Sized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith sourceClassL1

        // Construction alignment.
        val sourceL0 = sourceClassL0.constructors.single().call(listOf(19))
        val sourceL1 = sourceClassL1.constructors.single().call(listOf(sourceL0))
        val target = targetClass.constructorFrom(sourceClassL1).call(sourceL1)

        target["complex"]["list"]["size"] shouldBe 5
        target["complex"]["originalSize"] shouldBe sourceL1["complex"]["size"]

        target["complex"]["list"]["0"]["simple"]["list"]["size"] shouldBe 4
        target["complex"]["list"]["0"]["simple"]["originalSize"] shouldBe
            sourceL1["complex"]["0"]["simple"]["size"]
    }

    @Test
    fun `class with custom defaultable types must be sizeable`() {
        val loader = buildSizedVersion(
            SimpleSource(
                "Defaultable",
                """
                    class Defaultable(val n: Int) {
                        constructor() : this(0)
                    }
                """.trimMargin()
            ),
            SimpleSource(
                "ListWithDefault",
                """@Sized
                    class ListWithDefault(
                        val simple: @UseDefault Defaultable,
                        val shallow: @FixToLength(5) List<@UseDefault Defaultable>
                    )
                """.trimIndent()
            )
        )

        val defaultableClass = loader.load("Defaultable")
        val listWithDefaultClass = loader.load("ListWithDefault")
        val targetClass = loader.load("ListWithDefaultSized")

        // Structural integrity and alignment.
        targetClass shouldBeAlignedWith listWithDefaultClass

        // Construction alignment.
        val defaultable = defaultableClass
            .constructors
            .single { it.parameters.isNotEmpty() }
            .call(2)

        val listWithDefault = listWithDefaultClass.constructors.single().call(defaultable, listOf(defaultable))

        val target = targetClass.constructorFrom(listWithDefaultClass).call(listWithDefault)

        target["shallow"]["list"]["size"] shouldBe 5
        target["shallow"]["originalSize"] shouldBe listWithDefault["shallow"]["size"]

        // Check element we placed there.
        target["shallow"]["list"]["0"]["n"] shouldBe 2
        // Check filler element created by default.
        target["shallow"]["list"]["1"]["n"] shouldBe 0
    }

    private companion object {
        val sizedAnnotationPath = System.getProperty("user.dir") +
            "/src/main/kotlin/com/ing/zknotary/annotations/Sized.kt"

        fun buildSizedVersion(vararg simpleSource: SimpleSource): ClassLoader {
            val annotationSource = SourceFile.fromPath(File(sizedAnnotationPath))

            val targetSources = simpleSource.map {
                SourceFile.kotlin(
                    "${it.className}.kt",
                    "package com.ing.zknotary.annotations\n${it.source}"
                )
            }

            val compilation = KotlinCompilation().apply {
                sources = targetSources + annotationSource
                symbolProcessors = listOf(SizedProcessor())
            }

            val compiled = KSPRuntimeCompiler.compile(compilation)

            return ClassLoader(compiled.classLoader)
        }

        /**
         * Convenience class for sources, which will be packaged in an appropriate package.
         * Advantage is that class loading does not require the fully qualified name.
         */
        data class SimpleSource(val className: String, val source: String)

        data class ClassLoader(val classLoader: URLClassLoader) {
            fun load(className: String): KClass<out Any> =
                classLoader.loadClass("com.ing.zknotary.annotations.$className").kotlin
        }

        infix fun KClass<out Any>.shouldHaveSamePublicStructureWith(that: KClass<out Any>) {
            memberProperties.forEach {
                that shouldHaveMemberProperty it.name
            }
        }

        infix fun KClass<out Any>.shouldHaveConstructorsAlignedWith(that: KClass<out Any>) {
            require(constructors.any { it.parameters.isEmpty() }) {
                "${this.simpleName} must have an empty constructor"
            }

            require(constructors.any { it.parameters.size == that.memberProperties.size }) {
                "${this.simpleName} must have constructor with all public properties"
            }

            require(
                constructors.any {
                    it.parameters.size == 1 &&
                        it.parameters.first().type.asTypeName() == that.asTypeName()
                }
            ) {
                "${this.simpleName} must be constructable from $that"
            }
        }

        infix fun KClass<out Any>.shouldBeAlignedWith(that: KClass<out Any>) {
            this shouldHaveSamePublicStructureWith that
            this shouldHaveConstructorsAlignedWith that
        }

        fun KClass<out Any>.constructorFrom(that: KClass<out Any>): KFunction<Any> =
            constructors.single {
                it.parameters.size == 1 &&
                    it.parameters.first().type.asTypeName() == that.asTypeName()
            }

        operator fun Any.get(propertyName: String): Any {
            // Special cases to treat list
            if (this is List<*>) {
                // It seems impossible the `size` property of the kotlin.list using reflection.
                if (propertyName == "size") {
                    return this.size
                }

                val indexAccess = propertyName.toIntOrNull(10)
                if (indexAccess != null) {
                    return this[indexAccess]!!
                }
            }

            val thisProperty = this::class.memberProperties.first { it.name == propertyName } as KProperty1<Any, *>
            return thisProperty.get(this) ?: error("Property $propertyName is not found for ${Any::class.simpleName}")
        }
    }
}