package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflType.Companion.BITS_PER_BYTE
import com.ing.zinc.bfl.BflType.Companion.SERIALIZED_VAR
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.Self
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincConstant
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincTypeDef
import com.ing.zinc.poet.indent
import java.util.Locale
import java.util.Objects

@Suppress("TooManyFunctions")
open class BflStruct(
    override val id: String,
    val fields: List<Field>,
    override val customMethods: Collection<ZincFunction> = emptyList()
) : BflType, BflModule {
    init {
        fields.forEach {
            it.struct = this
        }
    }

    override fun typeName() = id

    override fun equals(other: Any?) =
        when (other) {
            is BflStruct -> id == other.id && fields == other.fields
            else -> false
        }

    override fun hashCode() = Objects.hash(id, fields)

    override fun toString(): String = "struct $id"

    open fun getModulesToImport() = fields.asSequence()
        .flatMap {
            when (val type = it.type) {
                is BflArray -> listOf(type.elementType)
                else -> listOf(type)
            }
        }
        .distinctBy { it.id }
        .filterIsInstance<BflModule>()
        .toList()

    /**
     * @property name The name of this field in the zinc struct (camel-case)
     * @property type The type of this field
     * @property struct The struct this field is a member of
     */
    data class Field(
        val name: String,
        val type: BflType,
    ) {
        internal var struct: BflStruct? = null

        /**
         * Generate a constant for this field with an optional [suffix].
         */
        fun generateConstant(suffix: String? = null): String {
            val actualSuffix = suffix?.let { "_${it.toUpperCase(Locale.getDefault())}" }
            return "${struct!!.id.camelToSnakeCase().toUpperCase(Locale.getDefault())}_${name.toUpperCase(Locale.getDefault())}$actualSuffix"
        }
    }

    override val bitSize: Int = fields.sumOf { it.type.bitSize }

    override fun deserializeExpr(options: DeserializationOptions): String {
        return options.deserializeModule(this)
    }

    override fun defaultExpr(): String = "$id::empty()"

    override fun equalsExpr(self: String, other: String): String =
        "$self.equals($other)"

    override fun sizeExpr() = getLengthConstant()

    override fun accept(visitor: TypeVisitor) {
        fields.forEach {
            visitor.visitType(it.type)
        }
    }

    internal open fun generateFieldDeserialization(
        witnessGroupOptions: WitnessGroupOptions,
        field: Field,
        witnessIndex: String?
    ): String {
        val offset = field.generateConstant("offset") + (witnessIndex?.let { " + $it" } ?: "")
        val deserializedField = field.type.deserializeExpr(
            DeserializationOptions(witnessGroupOptions, SERIALIZED_VAR, offset, field.name)
        )
        return "let ${field.name}: ${field.type.id} = $deserializedField;"
    }

    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions) = ZincFile.zincFile {
        comment("$id module")
        newLine()
        val modulesToImport = getModulesToImport()
        for (mod in modulesToImport) {
            mod { module = mod.id.camelToSnakeCase() }
        }
        mod {
            module = "consts"
        }
        if (modulesToImport.isNotEmpty()) { newLine() }
        for (module in modulesToImport) {
            use { path = "${module.getModuleName()}::${module.id}" }
            use { path = "${module.getModuleName()}::${module.serializedTypeDef.getName()}" }
            use { path = "${module.getModuleName()}::${module.getLengthConstant()}" }
            newLine()
        }
        comment("field lengths")
        addAll(generateFieldLengthConstants())
        newLine()
        comment("field offsets")
        addAll(generateFieldOffsetConstants())
        newLine()
        comment("length: ${fields.fold(0) { acc, field -> acc + field.type.bitSize }} bit(s)")
        add(generateLengthConstant())
        newLine()
        add(generateByteLengthConstant())
        newLine()
        add(serializedTypeDef)
        newLine()
        struct {
            name = id
            for (field in fields) {
                field {
                    name = field.name
                    type = field.type.toZincId()
                }
            }
        }
        newLine()
        impl {
            name = id
            addFunctions(this@BflStruct.getAllMethods(codeGenerationOptions).toList())
        }
    }

    @ZincMethod(order = 10)
    @Suppress("unused")
    open fun generateNewMethod(codeGenerationOptions: CodeGenerationOptions): ZincFunction {
        val fieldAssignments = fields.joinToString("\n") { "${it.name}: ${it.name}," }
        return zincFunction {
            name = "new"
            for (field in fields) {
                parameter { name = field.name; type = field.type.toZincId() }
            }
            returnType = Self
            comment = """
                Constructs a new $id, from the fields [${fields.joinToString { it.name }}] 
            """.trimIndent()
            body = """
                Self {
                    ${fieldAssignments.indent(20.spaces)}
                }
            """.trimIndent()
        }
    }

    @ZincMethod(order = 20)
    @Suppress("unused")
    open fun generateEmptyMethod(codeGenerationOptions: CodeGenerationOptions): ZincFunction {
        val defaultFields = fields.joinToString("\n") {
            "${it.name}: ${it.type.defaultExpr()},"
        }
        return zincFunction {
            name = "empty"
            returnType = Self
            comment = """
                Constructs a default instance of $id, with all fields set to default values. 
            """.trimIndent()
            body = """
                Self {
                    ${defaultFields.indent(20.spaces)}
                }
            """.trimIndent()
        }
    }

    open fun generateFieldEquals(field: Field) =
        "(${field.type.equalsExpr("self.${field.name}", "other.${field.name}")})"

    @ZincMethod(order = 30)
    @Suppress("unused")
    open fun generateEqualsMethod(codeGenerationOptions: CodeGenerationOptions) = zincMethod {
        name = "equals"
        parameter { name = "other"; type = Self }
        returnType = ZincPrimitive.Bool
        comment = """
            Checks whether `self` and `other` are equal.
        """.trimIndent()
        body = fields.joinToString("\n&& ") { generateFieldEquals(it) }
    }

    override val serializedTypeDef: ZincTypeDef = ZincTypeDef.zincTypeDef {
        name = "Serialized$id"
        type = zincArray {
            elementType = ZincPrimitive.Bool
            size = getLengthConstant()
        }
    }

    @ZincMethodList(order = 100)
    @Suppress("unused")
    fun generateDeserializeMethod(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return codeGenerationOptions.witnessGroupOptions.map {
            val fieldDeserializations = fields.fold(Pair("", 0)) { acc: Pair<String, Int>, field ->
                val implementation = generateFieldDeserialization(it, field, "offset")
                Pair(
                    "${acc.first}$implementation\n",
                    acc.second + field.type.bitSize
                )
            }.first
            val fieldAssignments = fields.joinToString("\n") { "${it.name}: ${it.name}," }
            zincFunction {
                name = it.deserializeMethodName
                parameter { name = SERIALIZED_VAR; type = it.witnessType }
                parameter { name = "offset"; type = ZincPrimitive.U24 }
                returnType = Self
                comment = """
                    Deserialize ${aOrAn()} $id from the ${it.witnessGroupName.capitalize()} group `$SERIALIZED_VAR`, at `offset`.
                """.trimIndent()
                body = """
                    ${fieldDeserializations.indent(20.spaces)}
                    Self {
                        ${fieldAssignments.indent(24.spaces)}
                    }
                """.trimIndent()
            }
        }
    }

    private fun generateFieldLengthConstants(): List<ZincConstant> =
        fields.map {
            ZincConstant.zincConstant {
                name = it.generateConstant("length")
                type = ZincPrimitive.U24
                initialization = it.type.sizeExpr()
                comment = "${it.name} field length: ${it.type.bitSize} bit(s)"
            }
        }

    private fun generateFieldOffsetConstants(): List<ZincConstant> {
        var previous: Field? = null
        return fields.map { field ->
            val itsPrevious = previous
            previous = field
            ZincConstant.zincConstant {
                name = field.generateConstant("offset")
                type = ZincPrimitive.U24
                initialization = itsPrevious?.let {
                    """
                        ${it.generateConstant("offset")}
                        + ${it.generateConstant("length")}
                    """.trimIndent()
                } ?: "0 as u24"
            }
        }
    }

    private fun generateLengthConstant(): ZincConstant {
        val lastField = fields.last()
        return ZincConstant.zincConstant {
            name = getLengthConstant()
            type = ZincPrimitive.U24
            initialization = """
                ${lastField.generateConstant("offset")}
                + ${lastField.generateConstant("length")}
            """.trimIndent()
        }
    }

    private fun generateByteLengthConstant(): ZincConstant {
        val byteSize = bitSize / BITS_PER_BYTE + if (bitSize % BITS_PER_BYTE == 0) 0 else 1
        return ZincConstant.zincConstant {
            name = getLengthConstant().replace("_LENGTH", "_BYTE_LENGTH")
            type = ZincPrimitive.U24
            initialization = "$byteSize"
        }
    }

    internal fun getRecursiveFields(): List<Field> {
        return fields.flatMap { field ->
            val fieldFields = (field.type as? BflStruct)?.let { struct ->
                struct.getRecursiveFields().map {
                    Field("${field.name}.${it.name}", it.type)
                }
            } ?: emptyList()
            fieldFields + field
        }
    }
}
