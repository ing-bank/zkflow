package com.ing.zkflow.serialization

import com.ing.zkflow.Surrogate
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
                    public class BasicTypesContainerKotlinxSurrogate(
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

                    public object BasicTypesContainerSerializer :
                        SurrogateSerializer<BasicTypesContainer, BasicTypesContainerKotlinxSurrogate>(BasicTypesContainerKotlinxSurrogate.serializer(),
                        { BasicTypesContainerKotlinxSurrogate(int = it.int, nullableInt = it.nullableInt) })
                """.trimIndent()
            ),

            TestCase(
                source = SourceFile.kotlin(
                    "CollectionsOfBasicNullableTypesContainer.kt",
                    """
                        package $packageName

                        import com.ing.zkflow.annotations.ZKP
                        import com.ing.zkflow.annotations.Size

                        @ZKP
                        data class CollectionsOfBasicNullableTypesContainer(
                            val myList: @Size(5) List<@Size(5) Map<Int, Int?>?>?,
                        )
                    """.trimIndent()
                ),
                expectedSerializationLocation = "CollectionsOfBasicNullableTypesContainer" + Surrogate.GENERATED_SERIALIZATION_FUNCTIONALITY_LOCATION_POSTFIX,
                expected = """
                    package $packageName

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
                    import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
                    import com.ing.zkflow.serialization.serializer.IntSerializer
                    import com.ing.zkflow.serialization.serializer.NullableSerializer
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                    import kotlin.Int
                    import kotlin.Suppress
                    import kotlin.collections.List
                    import kotlin.collections.Map
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable

                    @Suppress("ClassName")
                    @Serializable
                    public class CollectionsOfBasicNullableTypesContainerKotlinxSurrogate(
                      @Serializable(with = MyList_0::class)
                      public val myList: @Contextual List<@Contextual Map<@Contextual Int, @Contextual Int?>?>?
                    ) : Surrogate<CollectionsOfBasicNullableTypesContainer> {
                      public override fun toOriginal(): CollectionsOfBasicNullableTypesContainer =
                          CollectionsOfBasicNullableTypesContainer(myList)

                      private object MyList_0 : NullableSerializer<List<Map<Int, Int?>?>>(MyList_1)

                      private object MyList_1 : FixedLengthListSerializer<Map<Int, Int?>?>(5, MyList_2)

                      private object MyList_2 : NullableSerializer<Map<Int, Int?>>(MyList_3)

                      private object MyList_3 : FixedLengthMapSerializer<Int, Int?>(5, MyList_3_A_0, MyList_3_B_0)

                      private object MyList_3_A_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)

                      private object MyList_3_B_0 : NullableSerializer<Int>(MyList_3_B_1)

                      private object MyList_3_B_1 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
                    }

                    public object CollectionsOfBasicNullableTypesContainerSerializer :
                        SurrogateSerializer<CollectionsOfBasicNullableTypesContainer, CollectionsOfBasicNullableTypesContainerKotlinxSurrogate>(CollectionsOfBasicNullableTypesContainerKotlinxSurrogate.serializer(),
                        { CollectionsOfBasicNullableTypesContainerKotlinxSurrogate(myList = it.myList) })
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
                public class OutOfReachKotlinxSurrogate(
                  @Serializable(with = Int_0::class)
                  public val int: @Contextual Int
                ) : Surrogate<OutOfReach> {
                  public override fun toOriginal(): OutOfReach = OutOfReachSurrogate(int).toOriginal()

                  private object Int_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
                }

                public object OutOfReachSerializer :
                    SurrogateSerializer<OutOfReach, OutOfReachKotlinxSurrogate>(OutOfReachKotlinxSurrogate.serializer(),
                     {
                      val representation = ConverterOutOfReach.from(it)
                      OutOfReachKotlinxSurrogate(int = representation.int)
                    }
                  )
                """.trimIndent()
            ),

            TestCase(
                source = SourceFile.kotlin(
                    "ZkpInsideZkp.kt",
                    """
                        package $packageName

                        import com.ing.zkflow.annotations.ZKP

                        @ZKP
                        data class ZkpInsideZkp(
                            val myClass: MyClass,
                        )

                        @ZKP
                        class MyClass(val int: Int)
                    """.trimIndent()
                ),
                expectedSerializationLocation = "ZkpInsideZkp" + Surrogate.GENERATED_SERIALIZATION_FUNCTIONALITY_LOCATION_POSTFIX,
                expected = """
                    package $packageName

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
                    import kotlin.Suppress
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable

                    @Suppress("ClassName")
                    @Serializable
                    public class ZkpInsideZkpKotlinxSurrogate(
                      @Serializable(with = MyClass_0::class)
                      public val myClass: @Contextual MyClass
                    ) : Surrogate<ZkpInsideZkp> {
                      public override fun toOriginal(): ZkpInsideZkp = ZkpInsideZkp(myClass)

                      private object MyClass_0 : WrappedFixedLengthKSerializer<MyClass>(MyClassSerializer,
                          MyClass::class.java.isEnum)
                    }

                    public object ZkpInsideZkpSerializer :
                        SurrogateSerializer<ZkpInsideZkp, ZkpInsideZkpKotlinxSurrogate>(ZkpInsideZkpKotlinxSurrogate.serializer(),
                        { ZkpInsideZkpKotlinxSurrogate(myClass = it.myClass) })
                """.trimIndent()
            ),

            TestCase(
                source = SourceFile.kotlin(
                    "ZkpSurrogateInsideZkp.kt",
                    """
                        package $packageName
                        
                        import com.ing.zkflow.Default
                        import com.ing.zkflow.annotations.ZKP
                        import com.ing.zkflow.annotations.ZKPSurrogate
                        import com.ing.zkflow.Via
                        import com.ing.zkflow.DefaultProvider
                        import com.ing.zkflow.ConversionProvider
                        import com.ing.zkflow.Surrogate
                        
                        
                        @ZKP
                        data class ZkpSurrogateInsideZkp(
                            val myClass: @Via<OutOfReachSurrogate> @Default<OutOfReach>(DefaultOutOfReach::class) OutOfReach?,
                        )
                        
                        @ZKPSurrogate(ConverterOutOfReach::class)
                        class OutOfReachSurrogate(
                            val int: Int
                        ) : Surrogate<OutOfReach> {
                            override fun toOriginal() = OutOfReach(int)
                        }
                        
                        object ConverterOutOfReach : ConversionProvider<OutOfReach, OutOfReachSurrogate> {
                            override fun from(original: OutOfReach) = OutOfReachSurrogate(original.int)
                        }
                        
                        object DefaultOutOfReach : DefaultProvider<OutOfReach> {
                            override val default = OutOfReach(5)
                        }
                        
                        data class OutOfReach(val int: Int)
                    """.trimIndent()
                ),
                expectedSerializationLocation = "ZkpSurrogateInsideZkp" + Surrogate.GENERATED_SERIALIZATION_FUNCTIONALITY_LOCATION_POSTFIX,
                expected = """
                    package $packageName

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.NullableSerializer
                    import com.ing.zkflow.serialization.serializer.SerializerWithDefault
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
                    import kotlin.Suppress
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable

                    @Suppress("ClassName")
                    @Serializable
                    public class ZkpSurrogateInsideZkpKotlinxSurrogate(
                      @Serializable(with = MyClass_0::class)
                      public val myClass: @Contextual OutOfReach?
                    ) : Surrogate<ZkpSurrogateInsideZkp> {
                      public override fun toOriginal(): ZkpSurrogateInsideZkp = ZkpSurrogateInsideZkp(myClass)

                      private object MyClass_0 : NullableSerializer<OutOfReach>(MyClass_1)

                      private object MyClass_1 : SerializerWithDefault<OutOfReach>(MyClass_2, DefaultOutOfReach.default)

                      private object MyClass_2 : WrappedFixedLengthKSerializer<OutOfReach>(OutOfReachSerializer,
                          OutOfReachSurrogate::class.java.isEnum)
                    }

                    public object ZkpSurrogateInsideZkpSerializer :
                        SurrogateSerializer<ZkpSurrogateInsideZkp, ZkpSurrogateInsideZkpKotlinxSurrogate>(ZkpSurrogateInsideZkpKotlinxSurrogate.serializer(),
                        { ZkpSurrogateInsideZkpKotlinxSurrogate(myClass = it.myClass) })
                """.trimIndent()
            ),

            TestCase(
                source = SourceFile.kotlin(
                    "Option.kt",
                    """
                        package $packageName
                        
                        import com.ing.zkflow.annotations.ZKP
                        
                        @ZKP
                        enum class Option {                        
                            FIRST,
                            SECOND
                        }
                    """.trimIndent()
                ),
                expectedSerializationLocation = "Option" + Surrogate.GENERATED_SERIALIZATION_FUNCTIONALITY_LOCATION_POSTFIX,
                expected = """
                    package $packageName

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.IntSerializer
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                    import kotlin.Int
                    import kotlin.Suppress
                    import kotlinx.serialization.Serializable
                    
                    @Suppress("ClassName")
                    @Serializable
                    public class OptionKotlinxSurrogate(
                      @Serializable(with = Ordinal_0::class)
                      public val ordinal: Int
                    ) : Surrogate<Option> {
                      public override fun toOriginal(): Option = Option.values().first { it.ordinal == ordinal }
                    
                      private object Ordinal_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
                    }
                    
                    public object OptionSerializer :
                        SurrogateSerializer<Option, OptionKotlinxSurrogate>(OptionKotlinxSurrogate.serializer(), {
                        OptionKotlinxSurrogate(ordinal = it.ordinal) })
                """.trimIndent()
            ),
        )
    }
}
