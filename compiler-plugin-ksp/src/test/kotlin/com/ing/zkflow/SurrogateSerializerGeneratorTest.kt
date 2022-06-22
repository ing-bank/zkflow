package com.ing.zkflow

import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKPAnnotatedProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream

internal class SurrogateSerializerGeneratorTest : ProcessorTest(ZKPAnnotatedProcessorProvider()) {
    @ParameterizedTest
    @MethodSource("testCases")
    fun `surrogates for basic types container must be produced`(testCase: TestCase) {
        val outputStream = ByteArrayOutputStream()
        val result = compile(testCase.source, outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK

        result.readGeneratedKotlinFile(packageName, testCase.expectedSurrogateName) shouldBe testCase.expected
    }

    data class TestCase(
        val source: List<SourceFile>,
        val expectedSurrogateName: String,
        val expected: String,
    ) {
        constructor(source: SourceFile, expectedSurrogateName: String, expected: String) :
            this(listOf(source), expectedSurrogateName, expected)
    }

    companion object {
        const val packageName = "com.ing.zkflow.testing"

        @JvmStatic
        fun testCases(): List<TestCase> = listOf(
            TestCase(
                source = SourceFile.kotlin(
                    "BasicTypesContainer.kt",
                    """
                        package $packageName
             
                        import com.ing.zkflow.annotations.ZKP
    
                        @ZKP
                        data class BasicTypesContainer(
                            val int: Int,
                            val nullableInt: Int?
                        )                       
                    """.trimIndent()
                ),
                expectedSurrogateName = "BasicTypesContainerKotlinxSurrogate",
                expected = """
                    package $packageName
    
                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.IntSerializer
                    import com.ing.zkflow.serialization.serializer.NullableSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                    import kotlin.Int
                    import kotlin.Suppress
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable
                    
                    @Serializable
                    @Suppress("ClassName")
                    public data class BasicTypesContainerKotlinxSurrogate(
                      @Serializable(with = Int_0::class)
                      public val int: @Contextual Int,
                      @Serializable(with = NullableInt_0::class)
                      public val nullableInt: @Contextual Int?
                    ) : Surrogate<BasicTypesContainer> {
                      public override fun toOriginal(): BasicTypesContainer = BasicTypesContainer(int, nullableInt)
                    
                      private object Int_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
                    
                      private object NullableInt_0 : NullableSerializer<Int>(NullableInt_1)
                    
                      private object NullableInt_1 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
                    }
                """.trimIndent()
            )
        )
    }
}
