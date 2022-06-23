package com.ing.zkflow.versioning

import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKPAnnotatedProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ZKPAnnotatedProcessorProviderTest : ProcessorTest(ZKPAnnotatedProcessorProvider()) {
    @Test
    fun `Stable id's must be generated for all VersionedContractStateGroup ContractStates`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFile, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<KClassSerializerProvider>() shouldContain """
                com.ing.zkflow.contract.ZebraV1SerializerProvider
                com.ing.zkflow.contract.ZebraSerializerProvider
        """.trimIndent()
    }

    @Test
    fun `version group members should implement group interface directly`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(memberImplementsGroupIndirectly, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.messages shouldContain "must implement version group"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `marker interfaces should extend VersionedContractStateGroup directly`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(indirectMarkerInterface, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "is an interface that extends a version marker interface. This is not allowed"
    }

    @Test
    fun `unversioned states and commands should throw and error`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(unversionedStatesKotlinFile, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages.shouldContain(
            "All ContractStates annotated with @ZKP or @ZKPSurrogate " +
                "must implement a version marker interface. The following do not: UnversionedHorse, UnversionedZebra"
        )
    }

    companion object {
        private val kotlinFile = SourceFile.kotlin(
            "StateVersions.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import net.corda.core.contracts.ContractState

                interface IZebra: VersionedContractStateGroup, ContractState

                @ZKP
                class ZebraV1(): IZebra

                @ZKP
                class Zebra(): IZebra {
                    constructor(zebraV1: ZebraV1): this()
                }
            """
        )

        private val memberImplementsGroupIndirectly = SourceFile.kotlin(
            "MemberImplementsGroupIndirectly.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import net.corda.core.contracts.ContractState

                interface VersionGroup: VersionedContractStateGroup, ContractState

                abstract class Indirection: VersionGroup

                @ZKP
                class Member: Indirection()
            """
        )

        private val indirectMarkerInterface = SourceFile.kotlin(
            "IndirectMarkerInterface.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.common.versioning.VersionedContractStateGroup

                interface AnimalVersionGroup: VersionedContractStateGroup
                interface Horse: AnimalVersionGroup
            """
        )

        private val unversionedStatesKotlinFile = SourceFile.kotlin(
            "UnversionedStates.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.contracts.ZKCommandData
                import net.corda.core.contracts.ContractState
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
    
                @ZKP
                class UnversionedHorse(): ContractState
                @ZKP
                class UnversionedZebra(): ContractState

                @ZKP
                class UnversionedCommand() : ZKCommandData
            """
        )
    }
}
