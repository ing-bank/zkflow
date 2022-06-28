package com.ing.zkflow.versioning

import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKPAnnotatedProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class VersionSortingTest : ProcessorTest(ZKPAnnotatedProcessorProvider()) {
    @Test
    fun `Version groups must be sorted correctly`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(correctVersionConstructors, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
        result.messages shouldContain "Sorted version group IZebra: ZebraV1, ZebraV2, Zebra"
    }

    @Test
    fun `members of version groups may have only one upgrade constructor`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(tooManyUpgradeConstructors, outputStream)

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "ZebraV2 should have exactly one upgrade constructor. Found 2."
    }

    @Test
    fun `there must be exactly one group member without an upgrade constructor`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(missingUpgradeConstructors, outputStream)

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages.shouldContain(
            "In version groups, there must be exactly one version group member without a constructor " +
                "for a previous version. Found 2 without constructor: ZebraV1, ZebraV2"
        )
    }

    @Test
    fun `members of version groups should not have circular upgrade routes`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(circularUpgradeConstructors, outputStream)

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages.shouldContain(
            "Each version group members should be constructable from a unique previous version. " +
                "Found multiple that are upgradable from the following previous versions: ZebraV2"
        )
    }

    companion object {
        private val circularUpgradeConstructors = SourceFile.kotlin(
            "CircularUpgradeConstructors.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import net.corda.core.contracts.ContractState

                interface VersionedZebra: VersionedContractStateGroup, ContractState

                @ZKP
                class ZebraV1: VersionedZebra

                @ZKP
                class ZebraV2(): VersionedZebra {
                    constructor(zebraV1: ZebraV1): this()
                }

                @ZKP
                class ZebraV3(): VersionedZebra {
                    constructor(zebraV2: ZebraV2): this()
                }

                @ZKP
                class ZebraV4(): VersionedZebra {
                    constructor(zebraV2: ZebraV2): this()
                }

                @ZKP
                class Zebra(): VersionedZebra {
                    constructor(zebraV2: ZebraV4): this()
                }
            """
        )

        private val missingUpgradeConstructors = SourceFile.kotlin(
            "MissingUpgradeConstructors.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import net.corda.core.contracts.ContractState

                interface IZebra: VersionedContractStateGroup, ContractState

                @ZKP
                class ZebraV1: IZebra

                @ZKP
                class ZebraV2: IZebra
                

                @ZKP
                class Zebra(): IZebra {
                    constructor(zebraV2: ZebraV2): this()
                }
            """
        )
        private val tooManyUpgradeConstructors = SourceFile.kotlin(
            "TooManyUpgradeConstructors.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.contracts.ZKCommandData
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup
                import net.corda.core.contracts.ContractState

                interface IZebra: VersionedContractStateGroup, ContractState

                @ZKP
                class ZebraV1: IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                }

                @ZKP
                class ZebraV2(): IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV1: ZebraV1): this()
                    constructor(zebraV1: ZebraV2): this()
                }

                @ZKP
                class Zebra(): IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV2: ZebraV2): this()
                }
            """
        )
        private val correctVersionConstructors = SourceFile.kotlin(
            "CorrectVersionConstructors.kt",
            """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.common.contracts.ZKCommandData
                import net.corda.core.contracts.ContractState
                import com.ing.zkflow.common.versioning.VersionedContractStateGroup

                interface IZebra: VersionedContractStateGroup, ContractState

                @ZKP
                class ZebraV1: IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                }

                @ZKP
                class ZebraV2(): IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV1: ZebraV1): this()
                    constructor(ignored: String): this()
                }

                @ZKP
                class Zebra(): IZebra {
                    override val participants: List<AnonymousParty> = emptyList()
                    constructor(zebraV2: ZebraV2): this()
                }
            """
        )
    }
}
