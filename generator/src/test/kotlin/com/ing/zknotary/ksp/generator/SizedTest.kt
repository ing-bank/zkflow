package com.ing.zknotary.ksp.generator

import com.ing.zknotary.ksp.SizedProcessor
import com.squareup.kotlinpoet.asTypeName
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessors
import io.kotest.matchers.reflection.shouldHaveMemberProperty
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
            "SimpleState",
            """
                @Sized
                class SimpleState(val simple: Int)
            """.trimIndent()
        )

        val sourceClass = loader.load("SimpleState")
        val targetClass = loader.load("SimpleStateSized")

        // sourceClass.members.forEach { println(it.name) }
        // targetClass.members.forEach { println(it.name) }

        // Structural integrity and alignment.
        targetClass shouldHaveSamePublicStructureWith sourceClass
        targetClass shouldHaveConstructorsAlignedWith sourceClass

        // Construction alignment.
        val source = sourceClass.constructors.single().call(100)
        val target = targetClass.constructorFrom(sourceClass).call(source)

        target.coincides(source, "simple")
    }

    @Test
    fun `class with list of ints must be sizeable`() {
        // val gen = buildSizedVersion(
        //     "ListState",
        //     """
        //         @Sized
        //         class ListState(
        //             val shallow: @FixToLength(7) List<Int>,
        //             val deep: @FixToLength(5) List<@FixToLength(2) List<Int>>
        //         )
        //     """.trimIndent()
        // )
        //
        // gen.shouldHaveMemberProperty("shallow") {
        //     it.returnType.shouldBeOfType<SizedList<*>>()
        // }

        // gen.shouldHaveMemberProperty("deep") {
        //     it.returnType.shouldBeOfType<SizedList<*>>()
        //
        //     val clazz = it.returnType as KClass<SizedList<out Any>>
        // }

        // assert(
        //     sized.shallow is SizedList<*> &&
        //         sized.shallow.list.size == 7 &&
        //         sized.shallow.originalSize == state.shallow.size
        // )
        // assert(
        //     sized.deep is SizedList<*> &&
        //         sized.deep.list.size == 5 &&
        //         sized.deep.originalSize == state.deep.size
        // )
    }

    companion object {
        private val sizedAnnotationPath = System.getProperty("user.dir") +
            "/src/main/kotlin/com/ing/zknotary/annotations/Sized.kt"

        private fun buildSizedVersion(className: String, source: String): ClassLoader {
            val annotationSource = SourceFile.fromPath(File(sizedAnnotationPath))
            val targetSource = SourceFile.kotlin(
                "$className.kt",
                "package com.ing.zknotary.annotations\n" +
                    source
            )

            val compilation = KotlinCompilation().apply {
                sources = listOf(annotationSource, targetSource)
                symbolProcessors = listOf(SizedProcessor())
            }

            val compiled = KSPRuntimeCompiler.compile(compilation)

            return ClassLoader(compiled.classLoader)
        }

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

        fun KClass<out Any>.constructorFrom(that: KClass<out Any>): KFunction<Any> =
            constructors.single {
                it.parameters.size == 1 &&
                    it.parameters.first().type.asTypeName() == that.asTypeName()
            }

        @Suppress("UNCHECKED_CAST")
        fun Any.coincides(that: Any, propertyName: String): Boolean {
            val thisProperty = this::class.memberProperties.first { it.name == propertyName } as KProperty1<Any, *>
            val thatProperty = that::class.memberProperties.first { it.name == propertyName } as KProperty1<Any, *>

            return thisProperty.get(this) == thatProperty.get(that)
        }
    }
}
