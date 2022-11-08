package com.ing.zkflow.serialization.zinc

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.SERIALIZED
import com.ing.zinc.bfl.allModules
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.bfl.generator.ZincGenerator.createZargoToml
import com.ing.zinc.bfl.generator.ZincGenerator.zincSourceFile
import com.ing.zinc.bfl.mod
import com.ing.zinc.bfl.use
import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF16
import com.ing.zkflow.annotations.UTF32
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPStable
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.zinc.serialization.json.toUnsignedBitString
import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import com.ing.zkflow.serialization.toTree
import com.ing.zkflow.util.STUB_FOR_TESTING
import com.ing.zkflow.util.bitSize
import com.ing.zkflow.util.ensureDirectory
import com.ing.zkflow.util.ensureFile
import com.ing.zkflow.util.runCommand
import com.ing.zkflow.zinc.poet.generate.ZincTypeGenerator
import com.ing.zkflow.zinc.poet.generate.ZincTypeGeneratorResolver
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.internal.writeText
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Path
import java.util.Random

class SerializationDeserializationTest {
    private val scheme: BinaryFixedLengthScheme = ByteBinaryFixedLengthScheme
    private val zincGenerator: ZincTypeGeneratorResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)

    @Test
    fun `wrapped Byte that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedByte(42)
        val inputSerializer = SerializationDeserializationTestCompanionWrappedByteSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe JsonPrimitive("42")
    }

    @Test
    fun `wrapped Short that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedShort(42)
        val inputSerializer = SerializationDeserializationTestCompanionWrappedShortSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe JsonPrimitive("42")
    }

    @Test
    fun `wrapped Int that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedInt(42)
        val inputSerializer = SerializationDeserializationTestCompanionWrappedIntSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe JsonPrimitive("42")
    }

    @Test
    fun `wrapped Long that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedLong(42)
        val inputSerializer = SerializationDeserializationTestCompanionWrappedLongSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe JsonPrimitive("42")
    }

    @Test
    fun `wrapped List of Ints that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedListOfInts(listOf(42))
        val inputSerializer = SerializationDeserializationTestCompanionWrappedListOfIntsSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe listOf(JsonPrimitive("42")).toJsonList(8, JsonPrimitive("0"))
    }

    @Test
    fun `wrapped ASCII string that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedAsciiString("a")
        val inputSerializer = SerializationDeserializationTestCompanionWrappedAsciiStringSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe listOf(JsonPrimitive("97")).toJsonList(8, JsonPrimitive("0"))
    }

    @Test
    fun `wrapped UTF-8 string that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedUtf8String("a")
        val inputSerializer = SerializationDeserializationTestCompanionWrappedUtf8StringSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe listOf(JsonPrimitive("97")).toJsonList(8, JsonPrimitive("0"))
    }

    @Test
    fun `wrapped UTF-16 string that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedUtf16String("a")
        val inputSerializer = SerializationDeserializationTestCompanionWrappedUtf16StringSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe listOf("-2", "-1", "0", "97").map(::JsonPrimitive).toJsonList(8, JsonPrimitive("0"))
    }

    @Test
    fun `wrapped UTF-32 string that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedUtf32String("a")
        val inputSerializer = SerializationDeserializationTestCompanionWrappedUtf32StringSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer)
        actual shouldBe listOf("0", "0", "0", "97").map(::JsonPrimitive).toJsonList(8, JsonPrimitive("0"))
    }

    @Test
    fun `wrapped MockAsset that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedMockAsset(MockAsset(TestIdentity.fresh("alice").party.anonymise()))
        val inputSerializer = SerializationDeserializationTestCompanionWrappedMockAssetSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer) as JsonObject
        actual["value"] shouldBe JsonPrimitive("${input.value.value}")
    }

    @Test
    fun `wrapped Float that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedFloat(123450.07f)
        val inputSerializer = SerializationDeserializationTestCompanionWrappedFloatSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer) as JsonObject
        actual shouldBe buildJsonObject {
            put("kind", JsonPrimitive("1"))
            put("sign", JsonPrimitive("1"))
            put("integer", listOf(0, 5, 4, 3, 2, 1).map { JsonPrimitive(it.toString()) }.toJsonList(39, JsonPrimitive("0")))
            put("fraction", listOf(0, 7).map { JsonPrimitive(it.toString()) }.toJsonList(46, JsonPrimitive("0")))
        }
    }

    @Test
    fun `wrapped Double that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedDouble(123450.06789)
        val inputSerializer = SerializationDeserializationTestCompanionWrappedDoubleSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer) as JsonObject
        actual shouldBe buildJsonObject {
            put("kind", JsonPrimitive("2"))
            put("sign", JsonPrimitive("1"))
            put("integer", listOf(0, 5, 4, 3, 2, 1).map { JsonPrimitive(it.toString()) }.toJsonList(309, JsonPrimitive("0")))
            put("fraction", listOf(0, 6, 7, 8, 9).map { JsonPrimitive(it.toString()) }.toJsonList(325, JsonPrimitive("0")))
        }
    }

    @Test
    fun `wrapped BigDecimal that is serialized and deserialized should equal the original`(@TempDir tempDir: Path) {
        val input = WrappedBigDecimal(BigDecimal("0123450.06789"))
        val inputSerializer = SerializationDeserializationTestCompanionWrappedBigDecimalSerializer
        val actual = tempDir.serializeAndDeserializeInZinc(input, inputSerializer) as JsonObject
        actual shouldBe buildJsonObject {
            put("kind", JsonPrimitive("3"))
            put("sign", JsonPrimitive("1"))
            put("integer", listOf(0, 5, 4, 3, 2, 1).map { JsonPrimitive(it.toString()) }.toJsonList(20, JsonPrimitive("0")))
            put("fraction", listOf(0, 6, 7, 8, 9).map { JsonPrimitive(it.toString()) }.toJsonList(6, JsonPrimitive("0")))
        }
    }

    private fun <T : WrappedValue<*>> Path.serializeAndDeserializeInZinc(input: T, inputSerializer: KSerializer<T>): JsonElement {
        val serializedInput = scheme.encodeToBinary(inputSerializer, input)

        // assert that the size of the serialized data equals the expected size
        val serializationDescriptorTree = toTree(inputSerializer.descriptor)
        serializationDescriptorTree.bitSize shouldBe (serializedInput.size * Byte.SIZE_BITS)

        val witness = buildJsonObject {
            put(WITNESS, JsonArray(serializedInput.toUnsignedBitString()))
        }
        val bflModule = generateCircuit(witness, input)

        // assert that the generated circuit accepts a witness with the serialized data
        val zincStructureTree = bflModule.toStructureTree()
        zincStructureTree.bitSize shouldBe serializationDescriptorTree.bitSize

        val (stdout, stderr) = runCommand("zargo run", timeoutInSeconds = 30)

        stderr shouldBe ""
        return Json.parseToJsonElement(stdout)
    }

    /**
     * Generates a circuit that deserializes and unwraps the witness.
     */
    private fun Path.generateCircuit(witness: JsonObject, input: WrappedValue<*>): BflModule {
        createZargoToml("${input::class.simpleName}")
        ensureDirectory("data")
            .ensureFile("witness.json")
            .writeText(witness.toString())
        val bflModule = zincGenerator.zincTypeOf(input::class)
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
            listOf(bflModule, bflModule.typeOfSingleStructField())
                .filterIsInstance<BflModule>()
                .forEach {
                    add(it.mod())
                    add(it.use())
                }
            newLine()
            function {
                name = "main"
                parameter {
                    name = WITNESS
                    type = witnessGroupOptions.witnessType
                }
                returnType = bflModule.typeOfSingleStructField().toZincType()
                body = "${bflModule.deserializeExpr(witnessGroupOptions, "0 as u24", SERIALIZED, WITNESS)}.value"
            }
        }
        return bflModule
    }

    companion object {
        const val WITNESS = "witness"

        fun BflType.typeOfSingleStructField(): BflType {
            return (this as BflStruct).fields.single().type
        }

        @ZKPStable
        interface WrappedValue<T> {
            val value: T
        }

        @ZKP
        data class WrappedByte(
            override val value: Byte
        ) : WrappedValue<Byte>

        @ZKP
        data class WrappedShort(
            override val value: Short
        ) : WrappedValue<Short>

        @ZKP
        data class WrappedInt(
            override val value: Int
        ) : WrappedValue<Int>

        @ZKP
        data class WrappedLong(
            override val value: Long
        ) : WrappedValue<Long>

        @ZKP
        data class WrappedListOfInts(
            override val value: @Size(8) List<Int>
        ) : WrappedValue<List<Int>>

        @ZKP
        data class WrappedAsciiString(
            override val value: @ASCII(8) String
        ) : WrappedValue<String>

        @ZKP
        data class WrappedUtf8String(
            override val value: @UTF8(8) String
        ) : WrappedValue<String>

        @ZKP
        data class WrappedUtf16String(
            override val value: @UTF16(8) String
        ) : WrappedValue<String>

        @ZKP
        data class WrappedUtf32String(
            override val value: @UTF32(8) String
        ) : WrappedValue<String>

        @ZKP
        class Move : ZKCommandData {
            override val metadata: ResolvedZKCommandMetadata
                get() = STUB_FOR_TESTING()

            override fun verifyPrivate(): String = """
                mod module_command_context;
                use module_command_context::CommandContext;
                
                fn verify(ctx: CommandContext) {
                    // TODO
                }
            """.trimIndent()
        }

        interface VersionedMockAsset : VersionedContractStateGroup, OwnableState

        @ZKP
        data class MockAsset(
            override val owner: @EdDSA AnonymousParty,
            val value: Int = Random().nextInt()
        ) : VersionedMockAsset {
            override val participants: List<AnonymousParty> = listOf(owner)

            override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
                require(newOwner is AnonymousParty)
                return CommandAndState(Move(), copy(owner = newOwner))
            }
        }

        @ZKP
        data class WrappedMockAsset(
            override val value: MockAsset
        ) : WrappedValue<MockAsset>

        @ZKP
        data class WrappedBigDecimal(
            override val value: @BigDecimalSize(20, 6) BigDecimal
        ) : WrappedValue<BigDecimal>

        @ZKP
        data class WrappedFloat(
            override val value: Float
        ) : WrappedValue<Float>

        @ZKP
        data class WrappedDouble(
            override val value: Double
        ) : WrappedValue<Double>
    }
}
