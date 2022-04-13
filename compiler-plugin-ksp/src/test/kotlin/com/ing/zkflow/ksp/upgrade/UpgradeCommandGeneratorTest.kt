package com.ing.zkflow.ksp.upgrade

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zkflow.ksp.CodeGeneratorStub
import com.ing.zkflow.ksp.KSNameStub
import com.squareup.kotlinpoet.ClassName
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class UpgradeCommandGeneratorTest {
    private val v1 = mockk<KSClassDeclaration>()
    private val v2 = mockk<KSClassDeclaration>()
    private val v1Name = KSNameStub("com.example.V1")
    private val v2Name = KSNameStub("com.example.V2")

    init {
        every { v1.qualifiedName } returns v1Name
        every { v1.simpleName } returns v1Name.simpleName
        every { v1.packageName } returns v1Name.packageName
        every { v2.qualifiedName } returns v2Name
        every { v2.simpleName } returns v2Name.simpleName
        every { v2.packageName } returns v2Name.packageName
    }

    private fun testSubject(outputStream: ByteArrayOutputStream) = UpgradeCommandGenerator(
        CodeGeneratorStub(outputStream),
    )

    @Test
    fun `process empty family should return empty list`() {
        val testSubject = testSubject(ByteArrayOutputStream())
        val actual = testSubject.process(
            mapOf(
                "Imaginary" to emptyList()
            )
        )
        actual shouldBe emptyList()
    }

    @Test
    fun `process family with single member should return empty list`() {
        val testSubject = testSubject(ByteArrayOutputStream())
        val actual = testSubject.process(
            mapOf(
                "Single" to listOf(v1)
            )
        )
        actual shouldBe emptyList()
    }

    @Test
    fun `process family with two members should generate upgrade`() {
        val generatedBytes = ByteArrayOutputStream()
        val testSubject = testSubject(generatedBytes)
        val actual = testSubject.process(
            mapOf(
                "Double" to listOf(v1, v2)
            )
        )
        actual shouldBe listOf(ClassName("com.example", "UpgradeV1ToV2"))

        generatedBytes.toString("UTF-8") shouldBe """
            package com.example
            
            import com.ing.zkflow.annotations.ZKP
            import com.ing.zkflow.common.contracts.ZKCommandData
            import com.ing.zkflow.common.versioning.Versioned
            import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
            import com.ing.zkflow.common.zkp.metadata.commandMetadata
            import kotlin.String
            
            public interface UpgradeV1ToV2I : Versioned

            @ZKP
            public class UpgradeV1ToV2 : ZKCommandData, UpgradeV1ToV2I {
              public override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                      circuit {
                          name = "upgrade_v_1_to_v_2"
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
            
        """.trimIndent()
    }
}
