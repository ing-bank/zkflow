package com.ing.zknotary.ksp.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessors
import io.kotest.matchers.reflection.shouldHaveFunction
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class SizedTest2 {
    class MyProcessor : SymbolProcessor {
        private lateinit var codeGenerator: CodeGenerator

        override fun finish() {
        }

        override fun init(
            options: Map<String, String>,
            kotlinVersion: KotlinVersion,
            codeGenerator: CodeGenerator,
            logger: KSPLogger
        ) {
            this.codeGenerator = codeGenerator
        }

        override fun process(resolver: Resolver) {
            val symbols = resolver.getSymbolsWithAnnotation("foo.bar.TestAnnotation")
            symbols.size shouldBe 1
            val klass = symbols.first()
            check(klass is KSClassDeclaration)
            val qName = klass.qualifiedName ?: error("should've found qualified name")
            val genPackage = "${qName.getQualifier()}.generated"
            val genClassName = "${qName.getShortName()}_Gen"
            codeGenerator.createNewFile(
                packageName = genPackage,
                fileName = genClassName
            ).bufferedWriter(Charsets.UTF_8).use {
                it.write(
                    """
                        package $genPackage
                        class $genClassName() {
                            fun test() : String { return "FOO" }
                        }
                    """.trimIndent()
                )
            }
        }
    }

    @Test
    fun processorGeneratedCodeIsVisible() {
        val annotation = SourceFile.kotlin(
            "TestAnnotation.kt",
            """
            package foo.bar
            annotation class TestAnnotation
            """.trimIndent()
        )
        val targetClass = SourceFile.kotlin(
            "AppCode.kt",
            """
            package foo.bar
            import foo.bar.generated.AppCode_Gen
            @TestAnnotation
            class AppCode {
                init {
                    // access generated code
                    AppCode_Gen()
                }
            }
            """.trimIndent()
        )

        // Somehow this does not actually compile, but only KSP-generates classes.
        val processor = MyProcessor()
        val firstPassCompiler = KotlinCompilation().apply {
            sources = listOf(annotation, targetClass)
            symbolProcessors = listOf(processor)
        }

        val result = firstPassCompiler.compile()
        result.exitCode shouldBe ExitCode.OK

        val result2 = KotlinCompilation().apply {
            sources = listOf(annotation, targetClass) + firstPassCompiler.kspGeneratedSourceFiles()
        }.compile()
        result2.exitCode shouldBe ExitCode.OK

        val klazz = assertClassLoadable(result2, "foo.bar.generated.AppCode_Gen")
        klazz shouldHaveFunction "test"
    }

    private fun KotlinCompilation.kspGeneratedSourceFiles(): List<SourceFile> {
        return kspSourcesDir.resolve("kotlin")
            .walk()
            .filter { it.isFile }
            .map { SourceFile.fromPath(it.absoluteFile) }
            .toList()
    }

    private fun assertClassLoadable(compileResult: KotlinCompilation.Result, className: String): KClass<out Any> {
        return try {
            val clazz = compileResult.classLoader.loadClass(className)
            clazz shouldNotBe null
            clazz.kotlin
        } catch (e: ClassNotFoundException) {
            Assertions.fail("Class $className could not be loaded")
        }
    }
}
