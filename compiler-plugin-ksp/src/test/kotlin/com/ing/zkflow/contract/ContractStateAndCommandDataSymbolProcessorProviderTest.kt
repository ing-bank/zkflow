package com.ing.zkflow.contract

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKPAnnotatedProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class ContractStateAndCommandDataSymbolProcessorProviderTest : ProcessorTest(
    listOf(ZKPAnnotatedProcessorProvider())
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
    fun `ZKTransactionProcessor should correctly detect ContractState classes`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFileWithContractStateClass, outputStream)

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
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
                import com.ing.zkflow.common.zkp.metadata.commandMetadata

                interface Cmd: VersionedContractStateGroup

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
                
                class Container {
                    @ZKP
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

        private val kotlinFileWithContractStateClass = SourceFile.kotlin(
            "TestState.kt",
            """
                package com.ing.zkflow.zktransaction
                
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.annotations.corda.EdDSA
                import net.corda.core.contracts.CommandAndState
                import net.corda.core.identity.AbstractParty
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import net.corda.core.contracts.OwnableState
                import net.corda.core.identity.AnonymousParty
                
                interface VersionedState: VersionedContractStateGroup

                @ZKP
                data class TestState(
                    override val owner: @EdDSA AnonymousParty
                ): VersionedState, OwnableState {
                    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
                        require(newOwner is AnonymousParty)
                        return CommandAndState(TestCommand(), copy(owner = newOwner))
                    }
            
                    override val participants: List<AnonymousParty> = listOf(owner)
                }
               
            """.trimIndent()
        )
    }
}
