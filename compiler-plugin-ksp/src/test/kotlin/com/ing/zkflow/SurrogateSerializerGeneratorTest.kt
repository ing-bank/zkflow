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
    fun `surrogates for a ZKP-annotated container of basic types must be produced`(testCase: TestCase) {
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
        constructor(source: SourceFile, expectedSerializationLocation: String, expected: String) :
            this(listOf(source), expectedSerializationLocation, expected)
    }

    companion object {
        const val packageName = "com.ing.zkflow.testing"

        @JvmStatic
        @Suppress("LongMethod")
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
                expectedSerializationLocation = "BasicTypesContainer" + Surrogate.GENERATED_SERIALIZATION_FUNCTIONALITY_LOCATION_POSTFIX,
                expected = """
                    package $packageName

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.IntSerializer
                    import com.ing.zkflow.serialization.serializer.NullableSerializer
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                    import kotlin.Int
                    import kotlin.Suppress
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable

                    @Suppress("ClassName")
                    @Serializable
                    private data class BasicTypesContainerKotlinxSurrogate(
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

                    public object BasicTypesContainerKotlinxSurrogateSerializer :
                        SurrogateSerializer<BasicTypesContainer, BasicTypesContainerKotlinxSurrogate>(BasicTypesContainerKotlinxSurrogate.serializer(),
                        { BasicTypesContainerKotlinxSurrogate(int = it.int, nullableInt = it.nullableInt) })
                """.trimIndent()
            ),

            TestCase(
                source = SourceFile.kotlin(
                    "OutOfReachSurrogate.kt",
                    """
                        package $packageName
                                     
                        import com.ing.zkflow.annotations.ZKPSurrogate
                        import com.ing.zkflow.ConversionProvider
                        import com.ing.zkflow.Surrogate
                            
                        @ZKPSurrogate(ConverterOutOfReach::class)
                        class OutOfReachSurrogate(
                            val int: Int
                        ) : Surrogate<OutOfReach> {
                            override fun toOriginal() = OutOfReach(int)
                        }                        
                        
                        object ConverterOutOfReach : ConversionProvider<OutOfReach, OutOfReachSurrogate> {
                            override fun from(original: OutOfReach) = OutOfReachSurrogate(original.int)
                        }
                        
                        data class OutOfReach(val int: Int)

                    """.trimIndent()
                ),
                expectedSerializationLocation = "OutOfReach" + Surrogate.GENERATED_SERIALIZATION_FUNCTIONALITY_LOCATION_POSTFIX,
                expected = """
                package com.ing.zkflow.testing

                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.serialization.serializer.IntSerializer
                import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                import kotlin.Int
                import kotlin.Suppress
                import kotlinx.serialization.Contextual
                import kotlinx.serialization.Serializable
                
                @Suppress("ClassName")
                @Serializable
                private data class OutOfReachKotlinxSurrogate(
                  @Serializable(with = Int_0::class)
                  public val int: @Contextual Int
                ) : Surrogate<OutOfReach> {
                  public override fun toOriginal(): OutOfReach = OutOfReachSurrogate(int).toOriginal()
                
                  private object Int_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
                }
                
                public object OutOfReachKotlinxSurrogateSerializer :
                    SurrogateSerializer<OutOfReach, OutOfReachKotlinxSurrogate>(OutOfReachKotlinxSurrogate.serializer(),
                     {
                      val representation = ConverterOutOfReach.from(it)
                      OutOfReachKotlinxSurrogate(int = representation.int)
                    }
                  )
                """.trimIndent()
            )
        )
    }
}
