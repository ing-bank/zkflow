package com.ing.zkflow.contract

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.StableIdVersionedSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class ContractStateAndCommandDataSymbolProcessorProviderTest : ProcessorTest(
    listOf(StableIdVersionedSymbolProcessorProvider())
) {
    @Test
    fun `ZKTransactionProcessor should correctly register commands`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFileWithCommand, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<KClassSerializerProvider>() shouldBe "com.ing.zkflow.zktransaction.TestCommandSerializerProvider"
    }

    @Test
    fun `ZKTransactionProcessor should correctly register commands for nested classes`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(nestedKotlinSource, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<KClassSerializerProvider>() shouldBe "com.ing.zkflow.zktransaction.ContainerTestNestedCommandSerializerProvider"
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
        result.getMetaInfServicesPath<ZKCommandData>().shouldNotExist()
    }

    @Test
    fun `ZKTransactionProcessor should correctly detect ZKContractState classes`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFileWithZKContractStateClass, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<KClassSerializerProvider>() shouldBe "com.ing.zkflow.zktransaction.TestStateSerializerProvider"
    }

    companion object {
        private val kotlinFileWithCommand = SourceFile.kotlin(
            "TestCommand.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.contracts.ZKCommandData
                import com.ing.zkflow.common.versioning.Versioned
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata

                interface Cmd: Versioned

                @ZKP
                class TestCommand: Cmd, ZKCommandData {
                    
                    @Transient
                    override val metadata: ResolvedZKCommandMetadata = commandMetadata {
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
                
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.contracts.ZKCommandData
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata
                import com.ing.zkflow.common.versioning.Versioned
                
                interface Cmd: Versioned

                class Container {
                    @ZKP
                    class TestNestedCommand: Cmd, ZKCommandData {
                        
                        @Transient
                        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
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

        private val kotlinFileWithZKContractStateClass = SourceFile.kotlin(
            "TestState.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.contracts.ZKOwnableState
                import net.corda.core.contracts.CommandAndState
                import net.corda.core.identity.AnonymousParty
                import com.ing.zkflow.common.versioning.Versioned
                
                interface VersionedState: Versioned

                @ZKP
                data class TestState(
                    override val owner: AnonymousParty
                ): VersionedState, ZKOwnableState {
                    override fun withNewOwner(newOwner: AnonymousParty): CommandAndState {
                        return CommandAndState(TestCommand(), copy(owner = newOwner))
                    }
            
                    override val participants: List<AnonymousParty> = listOf(owner)
                }
               
            """.trimIndent()
        )
    }
}
