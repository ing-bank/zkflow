package com.ing.zkflow

import com.ing.zkflow.common.serialization.SurrogateSerializerRegistryProvider
import com.ing.zkflow.ksp.ProcessorTest
import com.ing.zkflow.processors.ZKFLowSymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
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

        val packageName = "com.ing.zkflow.serialization.infra"
        val className = "SomeClassIntSurrogateSurrogateSerializer"
        val providerClassName = SurrogateSerializerRegistryProvider::class.simpleName!!

        result.readGeneratedKotlinFile(packageName, className) shouldBe correctSourceExpectedOutput

        val metaInf = result.getGeneratedMetaInfServices<SurrogateSerializerRegistryProvider>()
        metaInf shouldStartWith "$packageName.$providerClassName"
    }

    @Test
    fun `SurrogateProcessor should fail surrogates with generics`() {
        val outputStream = ByteArrayOutputStream()
        val result = compile(listOf(sourceWith3PartyClass, incorrectSourceSurrogateMustBeFinal), outputStream)

        // In case of error, show output
        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            reportError(result, outputStream)
        }

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Surrogate implementers may not contain generics"
    }

    companion object {
        private val sourceWith3PartyClass = SourceFile.kotlin(
            "SomeClass.kt",
            """
                package com.ing.zkflow.testing

                data class SomeClass<T: Any>(val generic: T, val string: String)
            """.trimIndent()
        )

        private val correctSource = SourceFile.kotlin(
            "SomeClassIntSurrogate.kt",
            """
                package com.ing.zkflow.testing

                import com.ing.zkflow.ConversionProvider
                import com.ing.zkflow.testing.SomeClass
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.annotations.Ass
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

        private val correctSourceExpectedOutput = """
            package com.ing.zkflow.serialization.infra

            import com.ing.zkflow.serialization.serializer.SurrogateSerializer
            import com.ing.zkflow.testing.SomeClass
            import com.ing.zkflow.testing.SomeClassIntSurrogate
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
                import com.ing.zkflow.annotations.ASCII
                import com.ing.zkflow.Surrogate
                import com.ing.zkflow.annotations.ZKP

                @ZKP
                data class SomeClassGenericSurrogate<T>(
                    val generic: T,
                    val string: @ASCII(10) String
                ): Surrogate<SomeClass<T>> {
                    override fun toOriginal(): String {
                        TODO("Not yet implemented")
                    }
                }
            """
        )
    }
}
