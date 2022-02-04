package com.ing.zkflow.contract

import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.serialization.ZKContractStateSerializerMapProvider
import com.ing.zkflow.serialization.ZkCommandDataSerializerMapProvider
import com.ing.zkflow.stateandcommanddata.ContractStateAndCommandDataSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import net.corda.core.internal.readText
import org.junit.jupiter.api.Test
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

internal class ContractStateAndCommandDataSymbolProcessorProviderTest {
    @Test
    fun `ZKTransactionProcessor should correctly register stuff`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFileWithCommand, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<ZKTransactionMetadataCommandData>() shouldBe "com.ing.zkflow.zktransaction.TestCommand"
        result.getGeneratedMetaInfServices<ZkCommandDataSerializerMapProvider>() shouldStartWith "com.ing.zkflow.serialization.CommandDataSerializerMapProvider"
    }

    @Test
    fun `ZKTransactionProcessor should correctly register stuff for nested classes`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(nestedKotlinSource, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<ZKTransactionMetadataCommandData>() shouldBe "com.ing.zkflow.zktransaction.Container\$TestNestedCommand"
        result.getGeneratedMetaInfServices<ZkCommandDataSerializerMapProvider>() shouldStartWith "com.ing.zkflow.serialization.CommandDataSerializerMapProvider"
    }

    @Test
    fun `ZKTransactionProcessor should ignore classes that do not implement ZKTransactionMetadataCommandData`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(regularKotlinSource, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getMetaInfServicesPath<ZKTransactionMetadataCommandData>().shouldNotExist()
    }

    @Test
    fun `ZKTransactionProcessor should correctly detect state classes`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFileWithStateClass, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<ZKContractStateSerializerMapProvider>() shouldStartWith "com.ing.zkflow.serialization.ContractStateSerializerMapProvider"
    }

    private fun compile(
        kotlinSource: SourceFile,
        outputStream: ByteArrayOutputStream
    ) = KotlinCompilation().apply {
        sources = listOf(kotlinSource)

        symbolProcessorProviders = listOf(ContractStateAndCommandDataSymbolProcessorProvider())

        inheritClassPath = true
        messageOutputStream = BufferedOutputStream(outputStream) // see diagnostics in real time
    }.compile()

    private fun reportError(result: KotlinCompilation.Result, outputStream: ByteArrayOutputStream) =
        println(
            """
            Compilation failed:
            Compilation messages: ${result.messages}
            Output stream: $outputStream
            """.trimIndent()
        )

    companion object {
        private inline fun <reified T : Any> KotlinCompilation.Result.getGeneratedMetaInfServices() =
            getMetaInfServicesPath<T>().readText(StandardCharsets.UTF_8)

        private inline fun <reified T : Any> KotlinCompilation.Result.getMetaInfServicesPath() =
            Paths.get("${outputDirectory.absolutePath}/../ksp/sources/resources/META-INF/services/${T::class.java.canonicalName}")

        private val kotlinFileWithCommand = SourceFile.kotlin(
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

        private val kotlinFileWithStateClass = SourceFile.kotlin(
            "TestState.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.common.contracts.ZKOwnableState
                import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata
                import com.ing.zkflow.common.zkp.metadata.transactionMetadata
                import net.corda.core.contracts.CommandAndState
                import net.corda.core.identity.AnonymousParty
                
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
                
                    data class TestState(
                        override val owner: AnonymousParty
                    ): ZKOwnableState {
                        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState {
                            return CommandAndState(TestCommand(), copy(owner = newOwner))
                        }
                
                        override val participants: List<AnonymousParty> = listOf(owner)
                    }
                }
            """.trimIndent()
        )
    }
}
