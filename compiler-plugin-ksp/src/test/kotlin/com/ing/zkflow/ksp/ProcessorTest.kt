package com.ing.zkflow.ksp

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ing.zkflow.util.Tree
import com.ing.zkflow.util.ifOrNull
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import net.corda.core.internal.isDirectory
import net.corda.core.internal.list
import net.corda.core.internal.readText
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths

abstract class ProcessorTest(
    private val processorProviders: List<SymbolProcessorProvider>
) {

    constructor(processorProvider: SymbolProcessorProvider) : this(listOf(processorProvider))

    protected fun compile(
        kotlinSource: SourceFile,
        outputStream: ByteArrayOutputStream,
    ) = compile(listOf(kotlinSource), outputStream)

    protected fun compile(
        kotlinSources: List<SourceFile>,
        outputStream: ByteArrayOutputStream
    ) = KotlinCompilation().apply {
        sources = kotlinSources

        symbolProcessorProviders = processorProviders

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
        protected inline fun <reified T : Any> KotlinCompilation.Result.getGeneratedMetaInfServices(): String =
            getMetaInfServicesPath<T>().readText(StandardCharsets.UTF_8)

        @JvmStatic
        protected inline fun <reified T : Any> KotlinCompilation.Result.getMetaInfServicesPath(): Path =
            Paths.get("${outputDirectory.absolutePath}/../ksp/sources/resources/META-INF/services/${T::class.java.canonicalName}")

        @JvmStatic
        protected fun KotlinCompilation.Result.getMetaInfServicesFolder(): Path =
            Paths.get("${outputDirectory.absolutePath}/../")

        @JvmStatic
        protected fun Path.listRecursively(): Tree<Path, Path>? = if (this.isDirectory()) {
            val directoryNodes = this@listRecursively.list().mapNotNull { path ->
                path.listRecursively()
            }
            ifOrNull(directoryNodes.isNotEmpty()) {
                Tree.node(this) {
                    directoryNodes.forEach { addNode(it) }
                }
            }
        } else {
            Tree.leaf(this)
        }

        @JvmStatic
        protected fun KotlinCompilation.Result.readGeneratedKotlinFile(packageName: String, className: String): String =
            Paths
                .get("${outputDirectory.absolutePath}/../ksp/sources/kotlin/${packageName.replace(".", "/")}/$className.kt")
                .readText(StandardCharsets.UTF_8)
                .trim()
    }
}
