package com.ing.zkflow.contract

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistryProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class ContractStateAndCommandDataSymbolProcessorProviderTest : ProcessorTest() {
    @Test
    fun `ZKTransactionProcessor should correctly register commands`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFileWithCommand, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<CommandDataSerializerRegistryProvider>() shouldStartWith "com.ing.zkflow.serialization.infra.ZKCommandDataSerializerRegistryProvider"
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
        result.getGeneratedMetaInfServices<CommandDataSerializerRegistryProvider>() shouldStartWith "com.ing.zkflow.serialization.infra.ZKCommandDataSerializerRegistryProvider"
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
        result.getGeneratedMetaInfServices<ContractStateSerializerRegistryProvider>() shouldStartWith "com.ing.zkflow.serialization.infra.ZKContractStateSerializerRegistryProvider"
    }

    companion object {
        private val kotlinFileWithCommand = SourceFile.kotlin(
            "TestCommand.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.common.contracts.ZKCommandData
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata

                class TestCommand: ZKCommandData {
                    
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
                
                import com.ing.zkflow.common.contracts.ZKCommandData
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata
                
                class Container {
                    class TestNestedCommand: ZKCommandData {
                        
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
                
                import com.ing.zkflow.common.contracts.ZKOwnableState
                import net.corda.core.contracts.CommandAndState
                import net.corda.core.identity.AnonymousParty
                
                data class TestState(
                    override val owner: AnonymousParty
                ): ZKOwnableState {
                    override fun withNewOwner(newOwner: AnonymousParty): CommandAndState {
                        return CommandAndState(TestCommand(), copy(owner = newOwner))
                    }
            
                    override val participants: List<AnonymousParty> = listOf(owner)
                }
               
            """.trimIndent()
        )
    }
}
