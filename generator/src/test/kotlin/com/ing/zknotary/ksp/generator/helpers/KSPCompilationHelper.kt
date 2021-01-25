package com.ing.zknotary.ksp.generator.helpers

import com.ing.zknotary.ksp.SizedProcessor
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessors
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass

object KSPCompilationHelper {
    private val sizedAnnotationPath = System.getProperty("user.dir") +
        "/src/main/kotlin/com/ing/zknotary/annotations/Sized.kt"
    private val fixturesPath = System.getProperty("user.dir") +
        "/src/test/kotlin/com/ing/zknotary/ksp/generator/fixtures"

    private val fixturesFiles: Array<File>? = File(fixturesPath).listFiles { file -> file.extension == "kt" }

    fun buildSizedVersion(vararg simpleSource: SimpleSource): ClassLoader {
        val annotationSource = SourceFile.fromPath(File(sizedAnnotationPath))
        val fixturesSources = fixturesFiles?.map { SourceFile.fromPath(it) } ?: emptyList()

        val targetSources = simpleSource.map {
            SourceFile.kotlin(
                "${it.className}.kt",
                "package com.ing.zknotary.annotations\n${it.source}"
            )
        }

        val compilation = KotlinCompilation().apply {
            sources = targetSources + annotationSource + fixturesSources
            symbolProcessors = listOf(SizedProcessor())
        }

        val compiled = KSPRuntimeCompiler.compile(compilation)

        return ClassLoader(compiled.classLoader)
    }

    /**
     * KSPCompilationHelper class for sources, which will be packaged in an appropriate package.
     * Advantage is that class loading does not require the fully qualified name.
     */
    data class SimpleSource(val className: String, val source: String)

    data class ClassLoader(val classLoader: URLClassLoader) {
        fun load(className: String): KClass<out Any> =
            classLoader.loadClass("com.ing.zknotary.annotations.$className").kotlin
    }
}
