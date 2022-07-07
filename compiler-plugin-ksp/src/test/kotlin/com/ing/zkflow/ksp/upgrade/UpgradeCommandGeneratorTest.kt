package com.ing.zkflow.ksp.upgrade

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.ksp.CodeGeneratorStub
import com.ing.zkflow.ksp.KSNameStub
import com.ing.zkflow.ksp.versioning.VersionedCommandIdGenerator
import com.ing.zkflow.processors.serialization.SerializerProviderGenerator.SerializableClassWithSourceFiles
import com.squareup.kotlinpoet.ClassName
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class UpgradeCommandGeneratorTest {
    private val v1File = mockk<KSFile>()
    private val v2File = mockk<KSFile>()
    private val v1 = mockk<KSClassDeclaration>()
    private val v2 = mockk<KSClassDeclaration>()
    private val v1Name = KSNameStub("com.example.V1")
    private val v2Name = KSNameStub("com.example.V2")

    init {
        every { v1.qualifiedName } returns v1Name
        every { v1.simpleName } returns v1Name.simpleName
        every { v1.packageName } returns v1Name.packageName
        every { v1.containingFile } returns v1File
        every { v2.qualifiedName } returns v2Name
        every { v2.simpleName } returns v2Name.simpleName
        every { v2.packageName } returns v2Name.packageName
        every { v2.containingFile } returns v2File
    }

    private fun testSubject(outputStream: ByteArrayOutputStream) = UpgradeCommandGenerator(
        CodeGeneratorStub(outputStream),
    )

    @Test
    fun `process empty family should return empty list`() {
        val testSubject = testSubject(ByteArrayOutputStream())
        val actual = testSubject.generateUpgradeCommands(listOf(emptyList()))
        actual shouldBe emptyMap()
    }

    @Test
    fun `process family with single member should return empty list`() {
        val testSubject = testSubject(ByteArrayOutputStream())
        val actual = testSubject.generateUpgradeCommands(listOf(listOf(v1)))
        actual shouldBe emptyMap()
    }

    @Test
    @Suppress("LongMethod")
    fun `process family with two members should generate upgrade`() {
        val generatedBytes = ByteArrayOutputStream()
        val testSubject = testSubject(generatedBytes)
        val actual = testSubject.generateUpgradeCommands(listOf(listOf(v1, v2)))
        actual shouldBe listOf(
            SerializableClassWithSourceFiles.Generated(
                ClassName("com.example", "UpgradePrivateV1ToPrivateV2"),
                listOf(v1File, v2File),
            ),
            SerializableClassWithSourceFiles.Generated(
                ClassName("com.example", "UpgradeAnyV1ToPublicV2"),
                listOf(v1File, v2File),
            )
        ).associateWith { VersionedCommandIdGenerator.generateId(it) }

        generatedBytes.toString("UTF-8") shouldBe """
            package com.example
            
            import com.ing.zkflow.annotations.ZKP
            import com.ing.zkflow.common.contracts.ZKUpgradeCommandData
            import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
            import com.ing.zkflow.common.zkp.metadata.commandMetadata
            import kotlin.String
            
            @ZKP
            public class UpgradePrivateV1ToPrivateV2 : ZKUpgradeCommandData {
              public override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                      circuit {
                          name = "upgrade_private_v_1_to_private_v_2"
                      }
                      numberOfSigners = 1
                      command = true
                      notary = true
                      inputs {
                          private(com.example.V1::class) at 0
                      }
                      outputs {
                          private(com.example.V2::class) at 0
                      }
                  }
            
              public override fun verifyPrivate(): String =
                  com.ing.zkflow.zinc.poet.generate.generateUpgradeVerification(metadata).generate()
            }
            package com.example
            
            import com.ing.zkflow.annotations.ZKP
            import com.ing.zkflow.common.contracts.ZKUpgradeCommandData
            import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
            import com.ing.zkflow.common.zkp.metadata.commandMetadata
            import kotlin.String
            
            @ZKP
            public class UpgradeAnyV1ToPublicV2 : ZKUpgradeCommandData {
              public override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                      circuit {
                          name = "upgrade_any_v_1_to_public_v_2"
                      }
                      numberOfSigners = 1
                      command = true
                      notary = true
                      inputs {
                          any(com.example.V1::class) at 0
                      }
                      outputs {
                          public(com.example.V2::class) at 0
                      }
                  }
            
              public override fun verifyPrivate(): String =
                  com.ing.zkflow.zinc.poet.generate.generateUpgradeVerification(metadata).generate()
            }
	
        """.trimIndent()
    }
}
