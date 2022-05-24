package com.ing.zkflow.serialization.test

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.allModules
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.mod
import com.ing.zinc.bfl.use
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType
import com.ing.zinc.poet.indent
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.serialization.zinc.json.toUnsignedBitString
import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.util.ensureDirectory
import com.ing.zkflow.util.ensureFile
import com.ing.zkflow.util.runCommand
import com.ing.zkflow.zinc.poet.generate.ZincTypeGenerator
import com.ing.zkflow.zinc.poet.generate.ZincTypeGeneratorResolver
import com.ing.zkflow.zinc.types.toJsonObject
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.internal.writeText
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.nio.file.Path

class SerializedBigDecimalTest {
    companion object {
        @ZKP
        data class WrappedBigDecimal(
            val value: @BigDecimalSize(10, 10) BigDecimal
        )

        @JvmStatic
        fun operationTestCases() = listOf(
            Arguments.of("1024.045", "590.3552", "plus", "1614.4002"),
            Arguments.of("1024.045", "-590.3552", "plus", "433.6898"),
            Arguments.of("-1024.045", "590.3552", "plus", "-433.6898"),
            Arguments.of("-1024.045", "-590.3552", "plus", "-1614.4002"),
            Arguments.of("1024.045", "590.3552", "minus", "433.6898"),
            Arguments.of("1024.045", "-590.3552", "minus", "1614.4002"),
            Arguments.of("-1024.045", "590.3552", "minus", "-1614.4002"),
            Arguments.of("-1024.045", "-590.3552", "minus", "-433.6898"),
        )

        @JvmStatic
        fun comparisonTestCases() = listOf(
            Arguments.of("1024.045", "590.3552", 1),
            Arguments.of("1024.045", "-590.3552", 1),
            Arguments.of("-1024.045", "590.3552", -1),
            Arguments.of("-1024.045", "-590.3552", -1),
            Arguments.of("-1024.045", "-1024.045", 0),
            Arguments.of("-1024.045", "1024.045", -1),
            Arguments.of("1024.045", "-1024.045", 1),
            Arguments.of("1024.045", "1024.045", 0),
            Arguments.of("-1024.045", "-1024.0451", 1),
            Arguments.of("1024.045", "1024.0451", -1),
        )

        @JvmStatic
        fun equalsTestCases() = listOf(
            Arguments.of("1024.045", "1024.045", true),
            Arguments.of("1024.045", "1025.045", false),
            Arguments.of("1024.045", "1024.044", false),
        )
    }

    private val scheme: BinaryFixedLengthScheme = ByteBinaryFixedLengthScheme
    private val zincGenerator: ZincTypeGeneratorResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)

    @ParameterizedTest
    @MethodSource("operationTestCases")
    fun performOperationTestCase(left: String, right: String, operation: String, result: String, @TempDir tempDir: Path) {
        val witness = generateWitness(
            WrappedBigDecimal(BigDecimal(left)),
            WrappedBigDecimal(BigDecimal(right))
        )
        tempDir.runOperation(witness) { bflModule ->
            "${bflModule.id}::new(a.value.$operation(b.value))"
        } as JsonObject shouldBe buildJsonObject {
            put("value", BigDecimal(result).toJsonObject(10, 10, 3))
        }
    }

    @ParameterizedTest
    @MethodSource("comparisonTestCases")
    fun performComparisonTestCase(left: String, right: String, result: Int, @TempDir tempDir: Path) {
        val witness = generateWitness(
            WrappedBigDecimal(BigDecimal(left)),
            WrappedBigDecimal(BigDecimal(right))
        )
        tempDir.runOperation(witness, { ZincPrimitive.I8 }) {
            "a.value.compare(b.value)"
        } shouldBe JsonPrimitive("$result")
    }

    @ParameterizedTest
    @MethodSource("equalsTestCases")
    fun performEqualityTestCase(left: String, right: String, result: Boolean, @TempDir tempDir: Path) {
        val witness = generateWitness(
            WrappedBigDecimal(BigDecimal(left)),
            WrappedBigDecimal(BigDecimal(right))
        )
        tempDir.runOperation(witness, { ZincPrimitive.Bool }) {
            "a.value.equals(b.value)"
        } shouldBe JsonPrimitive(result)
    }

    private fun generateWitness(
        left: WrappedBigDecimal,
        right: WrappedBigDecimal
    ): JsonObject = buildJsonObject {
        put("left", JsonArray(scheme.encodeToBinary(WrappedBigDecimal.serializer(), left).toUnsignedBitString()))
        put("right", JsonArray(scheme.encodeToBinary(WrappedBigDecimal.serializer(), right).toUnsignedBitString()))
    }

    private fun Path.runOperation(
        witness: JsonObject,
        resultType: (BflModule) -> ZincType = { it.toZincType() },
        operation: (BflModule) -> String,
    ): JsonElement {
        val bflModule = zincGenerator.zincTypeOf(WrappedBigDecimal::class)
        createZargoToml("${this::class.simpleName}")
        ensureDirectory("data")
            .ensureFile("witness.json")
            .writeText(witness.toString())
        val witnessGroupOptions = TransactionComponentOptions.wrapped("test", bflModule)
        val codeGenerationOptions = CodeGenerationOptions(listOf(witnessGroupOptions))
        zincSourceFile("consts.zn") {
            add(witnessGroupOptions.witnessSizeConstant)
        }
        bflModule.allModules {
            zincSourceFile(
                getModuleName() + ".zn",
                generateZincFile(codeGenerationOptions)
            )
        }
        zincSourceFile("main.zn") {
            mod { module = "consts" }
            newLine()
            listOf(bflModule)
                .forEach {
                    add(it.mod())
                    add(it.use())
                }
            newLine()
            function {
                name = "main"
                parameter {
                    name = "left"
                    type = witnessGroupOptions.witnessType
                }
                parameter {
                    name = "right"
                    type = witnessGroupOptions.witnessType
                }
                returnType = resultType(bflModule)
                body = """
                    let a = ${bflModule.deserializeExpr(witnessGroupOptions, "0 as u24", "left", "left").indent(20.spaces)};
                    let b = ${bflModule.deserializeExpr(witnessGroupOptions, "0 as u24", "right", "right").indent(20.spaces)};
                    ${operation(bflModule)}
                """.trimIndent()
            }
        }

        val (stdout, stderr) = runCommand("zargo run", timeoutInSeconds = 30)

        stderr shouldBe ""
        println(stdout)
        return Json.parseToJsonElement(stdout)
    }
}
