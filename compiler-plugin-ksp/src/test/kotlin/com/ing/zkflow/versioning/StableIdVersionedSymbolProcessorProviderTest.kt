package com.ing.zkflow.versioning

import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.StableIdVersionedSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class StableIdVersionedSymbolProcessorProviderTest : ProcessorTest(StableIdVersionedSymbolProcessorProvider()) {
    @Test
    fun `Stable id's must be generated for all Versioned ZKContractStates`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFile, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<KClassSerializerProvider>() shouldContain """
                com.ing.zkflow.contract.ZebraV1SerializerProvider
                com.ing.zkflow.contract.ZebraV2SerializerProvider
                com.ing.zkflow.contract.ZebraSerializerProvider
                com.ing.zkflow.contract.HorseV1SerializerProvider
                com.ing.zkflow.contract.HorseV2SerializerProvider
                com.ing.zkflow.contract.HorseSerializerProvider
        """.trimIndent()
    }

    @Test
    fun `ensure name name clashes are avoided`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(nameClashSource, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    @Test
    fun `version group members should implement group interface directly`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(memberImplementsGroupIndirectly, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "must implement version group"
    }

    @Test
    fun `marker interfaces should extend Versioned directly`() {
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
    fun `commands without @ZKP annotation are not allowed`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(unannotatedCommandData, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "ZebraCommand is a CommandData and must therefore be annotated with either @ZKP or @ZKPSurrogate"
    }

    @Test
    fun `contract states without @ZKP annotation are not allowed`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(unannotatedContractState, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "ZebraV1 is a ContractState and must therefore be annotated with either @ZKP or @ZKPSurrogate"
    }

    @Test
    fun `unversioned states and commands should throw and error`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(unversionedStatesKotlinFile, outputStream)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "All types annotated with @ZKP or @ZKPSurrogate must implement a version marker interface. " +
            "The following do not: UnversionedHorse, UnversionedZebra, UnversionedCommand"
    }

    companion object {
        private val kotlinFile = SourceFile.kotlin(
            "StateVersions.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.contracts.ZKContractState
                import com.ing.zkflow.common.contracts.ZKCommandData
                import com.ing.zkflow.common.versioning.Versioned

                interface IZebra: Versioned, ZKContractState
                interface IHorse: Versioned

                @ZKP
                class ZebraV1: IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                }

                @ZKP
                class ZebraV2(): IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV1: ZebraV1): this()
                }

                @ZKP
                class Zebra(): IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV2: ZebraV2): this()
                }

                @ZKP
                class HorseV1: ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                }

                @ZKP
                class HorseV2(): ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV1: HorseV1): this()
                }

                @ZKP
                class Horse(): ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV2: HorseV2): this()
                }
            """
        )

        private val memberImplementsGroupIndirectly = SourceFile.kotlin(
            "MemberImplementsGroupIndirectly.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.versioning.Versioned

                interface VersionGroup: Versioned

                abstract class Indirection: VersionGroup

                @ZKP
                class Member: Indirection()
            """
        )

        private val indirectMarkerInterface = SourceFile.kotlin(
            "IndirectMarkerInterface.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.common.versioning.Versioned

                interface AnimalVersionGroup: Versioned
                interface Horse: AnimalVersionGroup
            """
        )

        private val unannotatedCommandData = SourceFile.kotlin(
            "UnannotatedCommandData.kt",
            """
                package com.ing.zkflow.contract

                import net.corda.core.contracts.CommandData
    
                class ZebraCommand(): CommandData {
                }
            """
        )

        private val unannotatedContractState = SourceFile.kotlin(
            "UnannotatedContractState.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.common.contracts.ZKContractState
    
                class ZebraV1(): ZKContractState {
                    override val participants: List<AnonymousParty> = emptyList()
                }
            """
        )

        private val unversionedStatesKotlinFile = SourceFile.kotlin(
            "UnversionedStates.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.contracts.ZKCommandData
                import com.ing.zkflow.common.contracts.ZKContractState
                import com.ing.zkflow.common.versioning.Versioned
    
                interface IZebra: Versioned
                interface IHorse: Versioned
    
                @ZKP
                class ZebraV1(): ZKContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                }
    
                @ZKP
                class ZebraV2(): ZKContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV1: ZebraV1): this()
                }
    
                @ZKP
                class Zebra(): ZKContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV2: ZebraV2): this()
                }
    
                @ZKP
                class HorseV1(): ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                }
    
                @ZKP
                class HorseV2(): ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV1: HorseV1): this()
                }
    
                @ZKP
                class Horse(): ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV2: HorseV2): this()
                }
                
                @ZKP
                class UnversionedHorse(): ZKContractState
                @ZKP
                class UnversionedZebra(): ZKContractState

                @ZKP
                class UnversionedCommand() : ZKCommandData
            """
        )

        private val nameClashSource = SourceFile.kotlin(
            "NameClash.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.versioning.Versioned
                import com.ing.zkflow.common.contracts.ZKCommandData

                class ContainerA {
                    interface IIssue: Versioned

                    @ZKP
                    class Issue: IIssue, ZKCommandData
                }

                class ContainerB {
                    interface IIssue: Versioned, ZKCommandData

                    @ZKP
                    class Issue: IIssue 
                }
            """
        )
    }
}
