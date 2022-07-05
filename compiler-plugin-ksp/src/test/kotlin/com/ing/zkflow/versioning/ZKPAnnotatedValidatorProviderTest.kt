package com.ing.zkflow.versioning

import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKPAnnotatedValidatorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ZKPAnnotatedValidatorProviderTest : ProcessorTest(ZKPAnnotatedValidatorProvider()) {
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
    fun `ZKStable annotation may not be on concrete classes`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKPStable

                @ZKPStable
                class MyType(
                    val foo: Int
                )
            """
            ),
            outputStream
        )

        result.messages shouldContain "should be an interface or abstract class"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `Surrogates may not be CommandData`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.ConversionProvider
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.Via
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.annotations.ZKPSurrogate
                import net.corda.core.contracts.CommandData
                import net.corda.core.identity.AbstractParty

                @ZKPSurrogate(MyTypeSurrogateConverter::class)
                class MyType(
                    val foo: Int
                ): Surrogate<Int>, CommandData {
                    override val participants: List<AbstractParty>
                        get() = TODO("Not yet implemented")
                }

                class MyTypeSurrogateConverter: ConversionProvider<MyType, MyTypeSurrogate> {
                    override fun from(original: MyType): MyTypeSurrogate = MyTypeSurrogate(original.foo)
                }
            """
            ),
            outputStream
        )

        result.messages shouldContain "is a CommandData and should not be annotated with @ZKPSurrogate"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `Surrogates may not target CommandData`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.ConversionProvider
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.Via
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.annotations.ZKPSurrogate
                import net.corda.core.contracts.CommandData
                import net.corda.core.contracts.ContractState
                import net.corda.core.identity.AbstractParty

                class MyType(
                    val foo: Int
                ): CommandData

                @ZKPSurrogate(MyTypeSurrogateConverter::class)
                class MyTypeSurrogate(val foo: Int): Surrogate<MyType> {
                    override fun toOriginal(): MyType = MyType(foo)
                }

                class MyTypeSurrogateConverter: ConversionProvider<MyType, MyTypeSurrogate> {
                    override fun from(original: MyType): MyTypeSurrogate = MyTypeSurrogate(original.foo)
                }
            """
            ),
            outputStream
        )

        result.messages shouldContain "Surrogates should not target CommandDatas"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `Surrogates may not be ContractState`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.ConversionProvider
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.Via
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.annotations.ZKPSurrogate
                import net.corda.core.contracts.ContractState
                import net.corda.core.identity.AbstractParty

                @ZKPSurrogate(MyTypeSurrogateConverter::class)
                class MyType(
                    val foo: Int
                ): Surrogate<Int>, ContractState {
                    override val participants: List<AbstractParty>
                        get() = TODO("Not yet implemented")
                }

                class MyTypeSurrogateConverter: ConversionProvider<MyType, MyTypeSurrogate> {
                    override fun from(original: MyType): MyTypeSurrogate = MyTypeSurrogate(original.foo)
                }
            """
            ),
            outputStream
        )

        result.messages shouldContain "is a ContractState and should not be annotated with @ZKPSurrogate"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `Surrogates may not target ContractState`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.ConversionProvider
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.Via
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.annotations.ZKPSurrogate
                import net.corda.core.contracts.ContractState
                import net.corda.core.identity.AbstractParty

                class MyType(
                    val foo: Int
                ): ContractState {
                    override val participants: List<AbstractParty>
                        get() = TODO("Not yet implemented")
                }

                @ZKPSurrogate(MyTypeSurrogateConverter::class)
                class MyTypeSurrogate(val foo: Int): Surrogate<MyType> {
                    override fun toOriginal(): MyType = MyType(foo)
                }

                class MyTypeSurrogateConverter: ConversionProvider<MyType, MyTypeSurrogate> {
                    override fun from(original: MyType): MyTypeSurrogate = MyTypeSurrogate(original.foo)
                }
            """
            ),
            outputStream
        )

        result.messages shouldContain "Surrogates should not target ContractStates"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `@Via surrogate's target type must match annotated type`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.ConversionProvider
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.Via
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.annotations.ZKPSurrogate

                @ZKP
                class MyType(
                    val foo: @Via<StringSurrogate> Int
                )

                @ZKPSurrogate(StringSurrogateConverter::class)
                class StringSurrogate(val originalString: @ASCII String): Surrogate<String> {
                    override fun toOriginal(): String = originalString
                }

                class StringSurrogateConverter: ConversionProvider<String, StringSurrogate> {
                    override fun from(original: String): StringSurrogate = StringSurrogate(original)
                }
            """
            ),
            outputStream
        )

        result.messages shouldContain "Target type of surrogate `StringSurrogate` set with @Via annotation must be Int, but is 'String'"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `non-public ZKP-annotated types are not allowed`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                @ZKP
                private class MyType(
                    val foo: Int
                ) 
            """
            ),
            outputStream
        )

        result.messages shouldContain "Classes annotated with @ZKP or @ZKPSurrogate must be public"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `primary constructor parameters of ZKP-annotated types must be val or var`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                @ZKP
                class MyType(
                    foo: Int
                ) 
            """
            ),
            outputStream
        )

        result.messages shouldContain "All primary constructor parameters of classes annotated with @ZKP or @ZKPSurrogate must be a val or var"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `non-public vals are not allowed in primary constructor of ZKP-annotated types`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                @ZKP
                class MyType(
                    private val foo: Int
                ) 
            """
            ),
            outputStream
        )

        result.messages shouldContain "All primary constructor parameters of classes annotated with @ZKP or @ZKPSurrogate must be public"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `parent vals from stable parent are allowed in primary constructor of ZKP-annotated types`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Stable.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import com.ing.zkflow.annotations.ZKPStable

                @ZKPStable
                interface Foo<T: Any> {
                    val foo: T
                }

                @ZKP
                class MyType(
                    override val foo: Int
                ) : Foo<Int> 

            """
            ),
            outputStream
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    @Test
    fun `parent vals are not allowed in primary constructor of ZKP-annotated types`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                interface Foo<T: Any> {
                    val foo: T
                }
                @ZKP
                class MyType(
                    override val foo: Int
                ) : Foo<Int> 

            """
            ),
            outputStream
        )

        result.messages shouldContain "Properties defined in parent Foo were unsafely overridden in primary constructor"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `Concrete type parameters are allowed on ZKP-annotated types`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "Invalid.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                interface Foo<T: Any> {
                    val foo: T
                }
                @ZKP
                class MyType(
                    val myFoo: Int
                ) : Foo<Int> {
                    override val foo: Int = myFoo
                }

            """
            ),
            outputStream
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    @Test
    fun `Generic type parameters are not allowed on ZKP-annotated types`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "NoGenerics.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                @ZKP
                class MyType<T: Any>
            """
            ),
            outputStream
        )

        result.messages shouldContain "Classes annotated with @ZKP or @ZKPSurrogate may not have type parameters"
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `String types should be annotated`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "MissingAnnotation.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                @ZKP
                class MyType (
                    val field: String
                )
            """
            ),
            outputStream
        )

        result.messages.shouldContain("String requires one of the following annotations: @ASCII, @UTF8, @UTF16 or @UTF16")
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `Character types should be annotated`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "MissingAnnotation.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                @ZKP
                class MyType (
                    val field: Char
                )
            """
            ),
            outputStream
        )

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.messages.shouldContain("Char is a Char type and requires either a @ASCIIChar annotation or a @UnicodeChar annotation")
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `ByteArray types should be annotated`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "MissingAnnotation.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                @ZKP
                class MyType (
                    val field: ByteArray
                )
            """
            ),
            outputStream
        )

        result.messages.shouldContain("Missing @Size annotation on kotlin.ByteArray")
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `Collection types should be annotated`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "MissingAnnotation.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP

                @ZKP
                class MyType (
                    val field: List<Int>
                )
            """
            ),
            outputStream
        )

        result.messages.shouldContain("List is a collection/iterable type and requires a @Size annotation")
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `BigDecimal should be annotated`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "missingAnnotation.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import java.math.BigDecimal

                @ZKP
                class MyType (
                    val field: BigDecimal
                )
            """
            ),
            outputStream
        )

        result.messages.shouldContain("Missing @${BigDecimalSize::class.simpleName} annotation")
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `SecureHash should be annotated`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "missingAnnotation.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import net.corda.core.crypto.SecureHash

                @ZKP
                class MyType (
                    val field: SecureHash
                )
            """
            ),
            outputStream
        )

        result.messages.shouldContain("Missing algorithm annotation")
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `Party should be annotated`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "missingAnnotation.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKP
                import net.corda.core.identity.Party

                @ZKP
                class MyType (
                    val field: Party 
                )
            """
            ),
            outputStream
        )

        result.messages.shouldContain("Missing algorithm annotation")
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `@ZKPSurrogate annotated should implement Surrogate interface`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(
            SourceFile.kotlin(
                "surrogateDoesNotImplement.kt",
                """
                package com.ing.zkflow.contract

                import com.ing.zkflow.annotations.ZKPSurrogate

                @ZKPSurrogate
                class MySurrogate 
            """
            ),
            outputStream
        )

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages.shouldContain(
            "All @ZKPSurrogate-annotated classes should implement the Surrogate interface"
        )
    }

    companion object {
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
