package com.ing.zkflow

import com.ing.zkflow.common.serialization.SurrogateSerializerRegistryProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKFLowSymbolProcessorProvider
import com.ing.zkflow.util.toStringTree
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.InternalPlatformDsl.toStr
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class SurrogateSerializerGeneratorTest : ProcessorTest(ZKFLowSymbolProcessorProvider()) {
    @Test
    fun `SurrogateProcessor should correctly register surrogates`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(listOf(sourceWith3PartyClass, correctSource), outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK

        println(result.getMetaInfServicesFolder().listRecursively()!!.toStringTree { fileName.toStr() })

        result.getGeneratedMetaInfServices<SurrogateSerializerRegistryProvider>() shouldBe "$packageName.$serializerProviderClassname"
        result.readGeneratedKotlinFile(packageName, serializerProviderClassname) shouldBe correctSourceExpectedOutput
    }

    @Test
    fun `SurrogateProcessor should correctly register surrogates with generics`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(listOf(sourceWith3PartyClassAndGenerics, correctSourceWithGenerics), outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK

        println(result.getMetaInfServicesFolder().listRecursively()!!.toStringTree { fileName.toStr() })

        result.getMetaInfServicesPath<SurrogateSerializerRegistryProvider>().shouldNotExist()
        result.readGeneratedKotlinFile(packageName, serializerProviderClassname) shouldBe correctSourceExpectedOutputWithGenerics
    }

    @Test
    fun `SurrogateProcessor should fail surrogates with generics`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(listOf(sourceWith3PartyClassAndGenerics, incorrectSourceSurrogateMustBeFinal), outputStream)

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Surrogate implementers may not contain generics"
    }

    companion object {
        const val packageName = "com.ing.zkflow.testing"
        const val serializerProviderClassname = "SomeClassIntSurrogateSerializerProvider"

        private val sourceWith3PartyClass = SourceFile.kotlin(
            "SomeClass.kt",
            """
                package com.ing.zkflow.testing

                data class SomeClass(val integer: Int, val string: String)
            """.trimIndent()
        )

        private val correctSource = SourceFile.kotlin(
            "SomeClassIntSurrogate.kt",
            """
                package com.ing.zkflow.testing

                import com.ing.zkflow.ConversionProvider
                import com.ing.zkflow.testing.SomeClass
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.annotations.ZKPSurrogate
                
                @ZKPSurrogate(SomeClassConverter::class)
                data class SomeClassIntSurrogate(
                    val integer: Int,
                    val string: @ASCII(10) String
                ): Surrogate<SomeClass> {
                    override fun toOriginal(): SomeClass {
                        TODO("Not yet implemented")
                    }
                }

                object SomeClassConverter: ConversionProvider<SomeClass, SomeClassIntSurrogate> {
                    override fun from(original: SomeClass) = SomeClassIntSurrogate(original.integer, original.string)
                }
            """
        )

        private val correctSourceExpectedOutput = """
            package com.ing.zkflow.testing
            
            import com.ing.zkflow.common.serialization.KClassSerializer
            import com.ing.zkflow.common.serialization.SurrogateSerializerRegistryProvider
            import com.ing.zkflow.serialization.serializer.SurrogateSerializer
            import kotlin.Any

            public object SomeClassIntSurrogateSurrogateSerializer :
                SurrogateSerializer<SomeClass, SomeClassIntSurrogate>(SomeClassIntSurrogate.serializer(), {
                com.ing.zkflow.testing.SomeClassConverter.from(it) })

            public class SomeClassIntSurrogateSerializerProvider : SurrogateSerializerRegistryProvider {
              public override fun `get`(): KClassSerializer<Any> = KClassSerializer(
              com.ing.zkflow.testing.SomeClass::class,
              199659197,
              com.ing.zkflow.testing.SomeClassIntSurrogateSurrogateSerializer
              )
            }
        """.trimIndent()

        private val sourceWith3PartyClassAndGenerics = SourceFile.kotlin(
            "SomeClass.kt",
            """
                package com.ing.zkflow.testing

                data class SomeClass<T: Any>(val generic: T, val string: String)
            """.trimIndent()
        )

        private val correctSourceWithGenerics = SourceFile.kotlin(
            "SomeClassIntSurrogate.kt",
            """
                package com.ing.zkflow.testing

                import com.ing.zkflow.ConversionProvider
                import com.ing.zkflow.testing.SomeClass
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.annotations.ZKPSurrogate
                
                @ZKPSurrogate(SomeClassConverter::class)
                data class SomeClassIntSurrogate(
                    val generic: Int,
                    val string: @ASCII(10) String
                ): Surrogate<SomeClass<Int>> {
                    override fun toOriginal(): SomeClass<Int> {
                        TODO("Not yet implemented")
                    }
                }

                object SomeClassConverter: ConversionProvider<SomeClass<Int>, SomeClassIntSurrogate> {
                    override fun from(original: SomeClass<Int>) = SomeClassIntSurrogate(original.generic, original.string)
                }
            """
        )

        private val correctSourceExpectedOutputWithGenerics = """
            package com.ing.zkflow.testing

            import com.ing.zkflow.serialization.serializer.SurrogateSerializer
            import kotlin.Int

            public object SomeClassIntSurrogateSurrogateSerializer :
                SurrogateSerializer<SomeClass<Int>, SomeClassIntSurrogate>(SomeClassIntSurrogate.serializer(), {
                com.ing.zkflow.testing.SomeClassConverter.from(it) })
        """.trimIndent()

        private val incorrectSourceSurrogateMustBeFinal = SourceFile.kotlin(
            "SomeClassGenericSurrogate.kt",
            """
                package com.ing.zkflow.testing

                import com.ing.zkflow.testing.SomeClass
                import com.ing.zkflow.annotations.Size
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.annotations.UTF8
                import com.ing.zkflow.annotations.ZKP

                @ZKP
                data class SomeClassGenericSurrogate<T>(
                    val generic: T,
                    val string: @UTF8(10) String
                ): Surrogate<SomeClass<T>> {
                    override fun toOriginal(): String {
                        TODO("Not yet implemented")
                    }
                }
            """
        )
    }
}
