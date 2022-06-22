package com.ing.zkflow.versioning

import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKPAnnotatedProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

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
                import com.ing.zkflow.common.contracts.ZKCommandData
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import net.corda.core.contracts.ContractState

                interface IZebra: VersionedContractStateGroup, ContractState
                interface IHorse: VersionedContractStateGroup

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
                class HorseV1: ContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                }

                @ZKP
                class HorseV2(): ContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV1: HorseV1): this()
                }

                @ZKP
                class Horse(): ContractState, IHorse {
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
    
                interface IZebra: VersionedContractStateGroup
                interface IHorse: VersionedContractStateGroup
    
                @ZKP
                class ZebraV1(): ContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                }
    
                @ZKP
                class ZebraV2(): ContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV1: ZebraV1): this()
                }
    
                @ZKP
                class Zebra(): ContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV2: ZebraV2): this()
                }
    
                @ZKP
                class HorseV1(): ContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                }
    
                @ZKP
                class HorseV2(): ContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV1: HorseV1): this()
                }
    
                @ZKP
                class Horse(): ContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV2: HorseV2): this()
                }
                
                @ZKP
                class UnversionedHorse(): ContractState
                @ZKP
                class UnversionedZebra(): ContractState

                @ZKP
                class UnversionedCommand() : ZKCommandData
            """
        )

        private val nameClashSource = SourceFile.kotlin(
            "NameClash.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import com.ing.zkflow.common.contracts.ZKCommandData

                class ContainerA {
                    interface IIssue: VersionedContractStateGroup

                    @ZKP
                    class Issue: IIssue, ZKCommandData
                }

                class ContainerB {
                    interface IIssue: VersionedContractStateGroup, ZKCommandData

                    @ZKP
                    class Issue: IIssue 
                }
            """
        )
    }
}
