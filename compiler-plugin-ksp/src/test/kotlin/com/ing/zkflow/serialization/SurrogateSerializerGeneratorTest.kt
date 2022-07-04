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

        result.readGeneratedKotlinFile("$packageName.generated", testCase.expectedSurrogateName) shouldBe testCase.expected
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
                    @file:Suppress(
                      "ClassName",
                      "DEPRECATION"
                    )

                    package $packageName.generated

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.IntSerializer
                    import com.ing.zkflow.serialization.serializer.NullableSerializer
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                    import $packageName.BasicTypesContainer
                    import kotlin.Int
                    import kotlin.Suppress
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable

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
                    @file:Suppress(
                      "ClassName",
                      "DEPRECATION"
                    )

                    package $packageName.generated

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
                    import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
                    import com.ing.zkflow.serialization.serializer.IntSerializer
                    import com.ing.zkflow.serialization.serializer.NullableSerializer
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                    import $packageName.CollectionsOfBasicNullableTypesContainer
                    import kotlin.Int
                    import kotlin.Suppress
                    import kotlin.collections.List
                    import kotlin.collections.Map
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable

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
                expectedSerializationLocation = "OutOfReachSurrogate" + Surrogate.GENERATED_SERIALIZATION_FUNCTIONALITY_LOCATION_POSTFIX,
                expected = """
                @file:Suppress(
                  "ClassName",
                  "DEPRECATION"
                )

                package $packageName.generated

                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.serialization.serializer.IntSerializer
                import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                import com.ing.zkflow.testing.ConverterOutOfReach
                import com.ing.zkflow.testing.OutOfReach
                import com.ing.zkflow.testing.OutOfReachSurrogate
                import kotlin.Int
                import kotlin.Suppress
                import kotlinx.serialization.Contextual
                import kotlinx.serialization.Serializable

                @Serializable
                public class OutOfReachSurrogateKotlinxSurrogate(
                  @Serializable(with = Int_0::class)
                  public val int: @Contextual Int
                ) : Surrogate<OutOfReach> {
                  public override fun toOriginal(): OutOfReach = OutOfReachSurrogate(int).toOriginal()

                  private object Int_0 : WrappedFixedLengthKSerializerWithDefault<Int>(IntSerializer)
                }

                public object OutOfReachSurrogateSerializer :
                    SurrogateSerializer<OutOfReach, OutOfReachSurrogateKotlinxSurrogate>(OutOfReachSurrogateKotlinxSurrogate.serializer(),
                     {
                      val representation = ConverterOutOfReach.from(it)
                      OutOfReachSurrogateKotlinxSurrogate(int = representation.int)
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
                    @file:Suppress(
                      "ClassName",
                      "DEPRECATION"
                    )

                    package $packageName.generated

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
                    import com.ing.zkflow.testing.MyClass
                    import com.ing.zkflow.testing.ZkpInsideZkp
                    import kotlin.Suppress
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable

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
                    @file:Suppress(
                      "ClassName",
                      "DEPRECATION"
                    )

                    package $packageName.generated

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.NullableSerializer
                    import com.ing.zkflow.serialization.serializer.SerializerWithDefault
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
                    import com.ing.zkflow.testing.DefaultOutOfReach
                    import com.ing.zkflow.testing.OutOfReach
                    import com.ing.zkflow.testing.OutOfReachSurrogate
                    import com.ing.zkflow.testing.ZkpSurrogateInsideZkp
                    import kotlin.Suppress
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable

                    @Serializable
                    public class ZkpSurrogateInsideZkpKotlinxSurrogate(
                      @Serializable(with = MyClass_0::class)
                      public val myClass: @Contextual OutOfReach?
                    ) : Surrogate<ZkpSurrogateInsideZkp> {
                      public override fun toOriginal(): ZkpSurrogateInsideZkp = ZkpSurrogateInsideZkp(myClass)

                      private object MyClass_0 : NullableSerializer<OutOfReach>(MyClass_1)

                      private object MyClass_1 : SerializerWithDefault<OutOfReach>(MyClass_2, DefaultOutOfReach.default)

                      private object MyClass_2 :
                          WrappedFixedLengthKSerializer<OutOfReach>(OutOfReachSurrogateSerializer,
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
                    @file:Suppress(
                      "ClassName",
                      "DEPRECATION"
                    )

                    package $packageName.generated

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.IntSerializer
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                    import com.ing.zkflow.testing.Option
                    import kotlin.Int
                    import kotlin.Suppress
                    import kotlinx.serialization.Serializable

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

            TestCase(
                source = SourceFile.kotlin(
                    "Parties.kt",
                    """
                    @file:Suppress("ClassName", "ArrayInDataClass")
                    package $packageName

                    import com.ing.zkflow.annotations.Size
                    import com.ing.zkflow.annotations.UTF8
                    import com.ing.zkflow.annotations.ZKP
                    import com.ing.zkflow.annotations.ZKPSurrogate
                    import com.ing.zkflow.annotations.corda.CordaX500NameSpec
                    import com.ing.zkflow.annotations.corda.EdDSA
                    import com.ing.zkflow.Via
                    import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
                    import com.ing.zkflow.testing.zkp.ZKNulls
                    import net.corda.core.crypto.Crypto
                    import net.corda.core.identity.AnonymousParty
                    import net.corda.core.identity.CordaX500Name
                    import net.corda.core.identity.Party
                    import java.security.PublicKey
                    import com.ing.zkflow.Surrogate

                    @ZKP
                    data class Parties(
                        val anonymousParty: @EdDSA AnonymousParty = someAnonymous,
                        val anonymousPartyFullyCustom: @Via<AnonymousParty_EdDSA> AnonymousParty = someAnonymous,

                        val party: @EdDSA Party = someParty,
                        val partyCX500Custom: @EdDSA @CordaX500NameSpec<CordaX500NameSurrogate>(CordaX500NameConverter::class) Party = someParty,
                        val partyFullyCustom: @Via<Party_EdDSA> Party = someParty,
                    ) {
                        companion object {
                            private val pk: PublicKey = PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)
                            val someAnonymous = AnonymousParty(pk)
                            val someParty = Party(CordaX500Name(organisation = "IN", locality = "AMS", country = "NL"), pk)
                        }
                    }

                    @ZKPSurrogate(PublicKey_EdDSA_Converter::class)
                    data class PublicKey_EdDSA(
                        val key: @Size(ED_DSA_KEY_LENGTH) ByteArray
                    ) : Surrogate<PublicKey> {
                        override fun toOriginal(): PublicKey =
                            KeyFactory
                                .getInstance("EdDSA")
                                .generatePublic(X509EncodedKeySpec(algorithmIdentifier + key))

                        companion object {
                            /**
                             * This is a hack to directly specify algorithm identifier to make the encoding agree with X509 specification.
                             * Specs can be found in [X509EncodedKeySpec] (/java/security/spec/X509EncodedKeySpec.java).
                             * This way direct construction is avoided, because it does not seem straightforward.
                             * Encoding construction can be found in net.i2p.crypto.eddsa.EdDSAPublicKey.getEncoded.
                             */
                            val algorithmIdentifier = byteArrayOf(48, 42, 48, 5, 6, 3, 43, 101, 112, 3, 33, 0)

                            const val ED_DSA_KEY_LENGTH = 32

                            const val ED_DSA_X509_ENCODING_LENGTH = 44
                        }
                    }

                    // There is an explanation to this converter in regular code, see `PublicKey_EdDSA_Converter`.
                    object PublicKey_EdDSA_Converter : ConversionProvider<PublicKey, PublicKey_EdDSA> {
                        override fun from(original: PublicKey): PublicKey_EdDSA {
                            val key = original.encoded.copyOfRange(
                                PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH - PublicKey_EdDSA.ED_DSA_KEY_LENGTH,
                                PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH
                            )
                            return PublicKey_EdDSA(key)
                        }
                    }

                    @ZKPSurrogate(Party_EdDSA_Converter::class)
                    data class Party_EdDSA(
                        val cordaX500Name: @UTF8(CordaX500NameSurrogate.UPPER_BOUND) String,
                        val encodedEdDSA: @Size(PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH) ByteArray
                    ) : Surrogate<Party> {
                        override fun toOriginal() = Party(
                            CordaX500Name.parse(cordaX500Name),
                            Crypto.decodePublicKey(Crypto.EDDSA_ED25519_SHA512, encodedEdDSA)
                        )
                    }

                    object Party_EdDSA_Converter : ConversionProvider<Party, Party_EdDSA> {
                        override fun from(original: Party): Party_EdDSA {
                            require(original.owningKey.algorithm == "EdDSA") {
                                "This converter only accepts parties with EdDSA keys"
                            }

                            return Party_EdDSA(
                                original.name.toString(),
                                original.owningKey.encoded
                            )
                        }
                    }

                    @ZKPSurrogate(AnonymousParty_EdDSA_Converter::class)
                    data class AnonymousParty_EdDSA(
                        val encodedEdDSA: @Size(PublicKey_EdDSA.ED_DSA_X509_ENCODING_LENGTH) ByteArray
                    ) : Surrogate<AnonymousParty> {
                        override fun toOriginal() = AnonymousParty(
                            Crypto.decodePublicKey(Crypto.EDDSA_ED25519_SHA512, encodedEdDSA)
                        )
                    }

                    object AnonymousParty_EdDSA_Converter : ConversionProvider<AnonymousParty, AnonymousParty_EdDSA> {
                        override fun from(original: AnonymousParty): AnonymousParty_EdDSA {
                            require(original.owningKey.algorithm == "EdDSA") {
                                "This converter only accepts parties with EdDSA keys"
                            }

                            return AnonymousParty_EdDSA(original.owningKey.encoded)
                        }
                    }

                    @ZKPSurrogate(CordaX500NameConverter::class)
                    data class CordaX500NameSurrogate(
                        val concat: @UTF8(UPPER_BOUND) String
                    ) : Surrogate<CordaX500Name> {
                        override fun toOriginal(): CordaX500Name =
                            CordaX500Name.parse(concat)

                        companion object {
                            const val UPPER_BOUND = 50
                        }
                    }

                    object CordaX500NameConverter : ConversionProvider<CordaX500Name, CordaX500NameSurrogate> {
                        override fun from(original: CordaX500Name): CordaX500NameSurrogate =
                            CordaX500NameSurrogate(original.toString())
                    }
                    """.trimIndent()
                ),
                expectedSerializationLocation = "Parties" + Surrogate.GENERATED_SERIALIZATION_FUNCTIONALITY_LOCATION_POSTFIX,
                expected = """
                    @file:Suppress(
                      "ClassName",
                      "DEPRECATION"
                    )

                    package $packageName.generated

                    import com.ing.zkflow.Surrogate
                    import com.ing.zkflow.serialization.serializer.SerializerWithDefault
                    import com.ing.zkflow.serialization.serializer.SurrogateSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
                    import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
                    import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
                    import com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer
                    import com.ing.zkflow.serialization.serializer.corda.PartySerializer
                    import com.ing.zkflow.testing.AnonymousParty_EdDSA
                    import com.ing.zkflow.testing.Parties
                    import com.ing.zkflow.testing.Party_EdDSA
                    import kotlin.Suppress
                    import kotlinx.serialization.Contextual
                    import kotlinx.serialization.Serializable
                    import net.corda.core.identity.AnonymousParty
                    import net.corda.core.identity.CordaX500Name
                    import net.corda.core.identity.Party

                    @Serializable
                    public class PartiesKotlinxSurrogate(
                      @Serializable(with = AnonymousParty_0::class)
                      public val anonymousParty: @Contextual AnonymousParty,
                      @Serializable(with = AnonymousPartyFullyCustom_0::class)
                      public val anonymousPartyFullyCustom: @Contextual AnonymousParty,
                      @Serializable(with = Party_0::class)
                      public val party: @Contextual Party,
                      @Serializable(with = PartyCX500Custom_0::class)
                      public val partyCX500Custom: @Contextual Party,
                      @Serializable(with = PartyFullyCustom_0::class)
                      public val partyFullyCustom: @Contextual Party
                    ) : Surrogate<Parties> {
                      public override fun toOriginal(): Parties = Parties(anonymousParty, anonymousPartyFullyCustom,
                          party, partyCX500Custom, partyFullyCustom)

                      private object AnonymousParty_0 : AnonymousPartySerializer(4)

                      private object AnonymousPartyFullyCustom_0 :
                          WrappedFixedLengthKSerializer<AnonymousParty>(AnonymousParty_EdDSASerializer,
                          AnonymousParty_EdDSA::class.java.isEnum)

                      private object Party_0 : PartySerializer(4, Party_1)

                      private object Party_1 :
                          WrappedFixedLengthKSerializerWithDefault<CordaX500Name>(CordaX500NameSerializer)

                      private object PartyCX500Custom_0 : PartySerializer(4, PartyCX500Custom_1)

                      private object PartyCX500Custom_1 :
                          SerializerWithDefault<CordaX500Name>(CordaX500NameSurrogateSerializer,
                          CordaX500NameSerializer.default)

                      private object PartyFullyCustom_0 : WrappedFixedLengthKSerializer<Party>(Party_EdDSASerializer,
                          Party_EdDSA::class.java.isEnum)
                    }

                    public object PartiesSerializer :
                        SurrogateSerializer<Parties, PartiesKotlinxSurrogate>(PartiesKotlinxSurrogate.serializer(), {
                        PartiesKotlinxSurrogate(anonymousParty = it.anonymousParty, anonymousPartyFullyCustom =
                        it.anonymousPartyFullyCustom, party = it.party, partyCX500Custom = it.partyCX500Custom,
                        partyFullyCustom = it.partyFullyCustom) })
                """.trimIndent()
            ),
        )
    }
}
