package com.ing.zkflow.zktransaction

import com.ing.zkflow.common.contracts.ZKTransactionMetadata
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
        result.getGeneratedMetaInfServices() shouldBe "com.ing.zkflow.zktransaction.Container.TestNestedCommand\n"
    }

    @Test
    fun `ZKTransactionProcessor should fail for classes that do not implement ZKTransactionMetadataCommandData`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(invalidKotlinSource, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            println(outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        outputStream.toString("UTF-8") shouldContain "The following classes are annotated with @${ZKTransactionMetadata::class.simpleName}," +
            " but don't implement ${ZKTransactionMetadataCommandData::class.qualifiedName}: com.ing.zkflow.zktransaction.InvalidZKCommand"
    }

    private fun compile(
        kotlinSource: SourceFile,
        outputStream: ByteArrayOutputStream
    ) = KotlinCompilation().apply {
        sources = listOf(kotlinSource)

        symbolProcessorProviders = listOf(ZKTransactionMetadataProcessorProvider())

        inheritClassPath = true
        messageOutputStream = BufferedOutputStream(outputStream) // see diagnostics in real time
    }.compile()

    companion object {
        private fun KotlinCompilation.Result.getGeneratedMetaInfServices() =
            Paths.get("${outputDirectory.absolutePath}/../ksp/sources/resources/META-INF/services/${ZKTransactionMetadataCommandData::class.java.canonicalName}")
                .readText(StandardCharsets.UTF_8)

        private val correctKotlinSource = SourceFile.kotlin(
            "TestCommand.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.common.contracts.ZKTransactionMetadata
                import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata
                import com.ing.zkflow.common.zkp.metadata.transactionMetadata

                @ZKTransactionMetadata
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
                
                import com.ing.zkflow.common.contracts.ZKTransactionMetadata
                import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata
                import com.ing.zkflow.common.zkp.metadata.transactionMetadata

                class Container {
                    @ZKTransactionMetadata
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

        private val invalidKotlinSource = SourceFile.kotlin(
            "InvalidZKCommand.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.common.contracts.ZKTransactionMetadata

                @ZKTransactionMetadata
                class InvalidZKCommand
            """
        )
    }
}
