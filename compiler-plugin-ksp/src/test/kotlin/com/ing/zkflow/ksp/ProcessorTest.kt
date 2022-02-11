package com.ing.zkflow.ksp

import com.ing.zkflow.processors.ZKFLowSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import net.corda.core.internal.readText
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths

abstract class ProcessorTest {
    protected fun compile(
        kotlinSource: SourceFile,
        outputStream: ByteArrayOutputStream
    ) = KotlinCompilation().apply {
        sources = listOf(kotlinSource)

        symbolProcessorProviders = listOf(ZKFLowSymbolProcessorProvider())

        inheritClassPath = true
        messageOutputStream = BufferedOutputStream(outputStream) // see diagnostics in real time
    }.compile()

    protected fun reportError(result: KotlinCompilation.Result, outputStream: ByteArrayOutputStream) =
        println(
            """
            Compilation failed:
            Compilation messages: ${result.messages}
            Output stream: $outputStream
            """.trimIndent()
        )

    companion object {
        @JvmStatic
        protected inline fun <reified T : Any> KotlinCompilation.Result.getGeneratedMetaInfServices(): String? =
            getMetaInfServicesPath<T>()?.readText(StandardCharsets.UTF_8)

        @JvmStatic
        protected inline fun <reified T : Any> KotlinCompilation.Result.getMetaInfServicesPath(): Path? =
            Paths.get("${outputDirectory.absolutePath}/../ksp/sources/resources/META-INF/services/${T::class.java.canonicalName}")
    }
}
