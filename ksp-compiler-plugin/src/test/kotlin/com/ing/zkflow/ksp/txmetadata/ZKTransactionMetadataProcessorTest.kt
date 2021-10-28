package com.ing.zkflow.ksp.txmetadata

import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.ksp.CompositeSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import net.corda.core.internal.readText
import org.junit.jupiter.api.Test
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

internal class ZKTransactionMetadataProcessorTest {
    @Test
    fun `ZKTransactionProcessor should correctly register stuff`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(correctKotlinSource, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            println(outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices() shouldBe "com.ing.zkflow.zktransaction.TestCommand\n"
    }

    @Test
    fun `ZKTransactionProcessor should correctly register stuff for nested classes`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(nestedKotlinSource, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            println(outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices() shouldBe "com.ing.zkflow.zktransaction.Container\$TestNestedCommand\n"
    }

    @Test
    fun `ZKTransactionProcessor should ignore classes that do not implement ZKTransactionMetadataCommandData`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(regularKotlinSource, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            println(outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getMetaInfServicesPath().shouldNotExist()
    }

    private fun compile(
        kotlinSource: SourceFile,
        outputStream: ByteArrayOutputStream
    ) = KotlinCompilation().apply {
        sources = listOf(kotlinSource)

        symbolProcessorProviders = listOf(CompositeSymbolProcessorProvider())

        inheritClassPath = true
        messageOutputStream = BufferedOutputStream(outputStream) // see diagnostics in real time
    }.compile()

    companion object {
        private fun KotlinCompilation.Result.getGeneratedMetaInfServices() =
            getMetaInfServicesPath().readText(StandardCharsets.UTF_8)

        private fun KotlinCompilation.Result.getMetaInfServicesPath() =
            Paths.get("${outputDirectory.absolutePath}/../ksp/sources/resources/META-INF/services/${ZKTransactionMetadataCommandData::class.java.canonicalName}")

        private val correctKotlinSource = SourceFile.kotlin(
            "TestCommand.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata
                import com.ing.zkflow.common.zkp.metadata.transactionMetadata

                class TestCommand: ZKTransactionMetadataCommandData {
                    override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
                        commands { +TestCommand::class }
                    }
                    
                    @Transient
                    override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                        private = true
                        circuit { name = "TestCommand" }
                        numberOfSigners = 1
                    }
                }
            """
        )

        private val nestedKotlinSource = SourceFile.kotlin(
            "TestNestedCommand.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata
                import com.ing.zkflow.common.zkp.metadata.transactionMetadata

                class Container {
                    class TestNestedCommand: ZKTransactionMetadataCommandData {
                        override val transactionMetadata: ResolvedZKTransactionMetadata by transactionMetadata {
                            commands { +TestNestedCommand::class }
                        }
                        
                        @Transient
                        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                            private = true
                            circuit { name = "TestNestedCommand" }
                            numberOfSigners = 1
                        }
                    }
                }
            """
        )

        private val regularKotlinSource = SourceFile.kotlin(
            "NotACommand.kt",
            """
                package com.ing.zkflow.zktransaction
                
                class NotACommand
            """
        )
    }
}
