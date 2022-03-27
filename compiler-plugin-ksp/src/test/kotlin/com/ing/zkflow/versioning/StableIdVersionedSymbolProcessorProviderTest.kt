package com.ing.zkflow.versioning

import com.ing.zkflow.common.serialization.ContractStateSerializerRegistryProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.StableIdVersionedSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class StableIdVersionedSymbolProcessorProviderTest : ProcessorTest(StableIdVersionedSymbolProcessorProvider()) {
    @Test
    fun `Stable id's must be generated for all Versioned ZKContractState's`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(kotlinFile, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.getGeneratedMetaInfServices<ContractStateSerializerRegistryProvider>() shouldStartWith
            "com.ing.zkflow.serialization.infra.ZKContractStateSerializerRegistryProvider"
    }

    companion object {
        private val kotlinFile = SourceFile.kotlin(
            "StateVersions.kt",
            """
                import com.ing.zkflow.common.contracts.ZKContractState
                import com.ing.zkflow.common.versioning.Versioned

                interface IZebra: Versioned
                interface IHorse: Versioned

                class ZebraV1(): ZKContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                }

                class ZebraV2(): ZKContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV1: ZebraV1): this()
                }

                class Zebra(): ZKContractState, IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV2: ZebraV2): this()
                }

                class HorseV1(): ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                }

                class HorseV2(): ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV1: HorseV1): this()
                }

                class Horse(): ZKContractState, IHorse {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(horseV2: HorseV2): this()
                }
            """
        )
    }
}
