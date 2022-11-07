package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincConstant
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincInvocable
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincStruct
import com.ing.zinc.poet.ZincStruct.Companion.zincStruct
import com.ing.zinc.poet.ZincType.Self
import com.ing.zinc.poet.indent
import com.ing.zkflow.util.BflSized
import com.ing.zkflow.util.NodeDescriptor
import com.ing.zkflow.util.Tree
import java.util.Objects

@Suppress("TooManyFunctions")
open class BflStruct(
    override val id: String,
    fields: List<BflStructField>,
    private val functions: List<ZincInvocable>,
    val isDeserializable: Boolean,
    private val additionalImports: List<BflModule>
) : BflModule {
    constructor(id: String, fields: List<BflStructField>) : this(id, fields, emptyList(), true, emptyList())
    constructor(id: String, fields: List<BflStructField>, isDeserializable: Boolean) : this(id, fields, emptyList(), isDeserializable, emptyList())

    val fields: List<FieldWithParentStruct> = fields.map {
        FieldWithParentStruct(it.name, it.type, this)
    }

    fun copyWithAdditionalFunctionsAndImports(
        additionalFunctions: List<ZincFunction>,
        extraImports: List<BflModule>
    ): BflStruct {
        require(this::class == BflStruct::class) {
            "Copy with additional functions is only allowed on ${BflStruct::class.simpleName}"
        }
        return BflStruct(id, fields, functions + additionalFunctions, isDeserializable, additionalImports + extraImports)
    }

    override fun typeName() = id

    override fun equals(other: Any?) =
        when (other) {
            is BflStruct ->
                id == other.id &&
                    fields == other.fields &&
                    isDeserializable == other.isDeserializable
            else -> false
        }

    override fun hashCode() = Objects.hash(id, fields, isDeserializable)

    override fun toString(): String = """
        struct $id {
            ${fields.joinToString("\n") { "${it.name}: ${it.type}," }.indent(12.spaces)}
        }
    """.trimIndent()

    open fun getModulesToImport() = (getFieldTypesToImport() + additionalImports)
        .distinctBy { it.id }
        .filterIsInstance<BflModule>()
        .toList()

    private fun getFieldTypesToImport() = fields.asSequence()
        .flatMap {
            when (val type = it.type) {
                is BflArray -> sequenceOf(type.elementType)
                else -> sequenceOf(type)
            }
        }

    override val bitSize: Int = this.fields.sumBy { it.type.bitSize }

    override fun deserializeExpr(
        transactionComponentOptions: TransactionComponentOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String = "$id::${transactionComponentOptions.deserializeMethodName}($witnessVariable, $offset)"

    override fun defaultExpr(): String = "$id::empty()"

    override fun equalsExpr(self: String, other: String): String =
        "$self.equals($other)"

    override fun sizeExpr() = getLengthConstant()

    override fun accept(visitor: TypeVisitor) {
        fields.forEach {
            visitor.visitType(it.type)
        }
        additionalImports.forEach { visitor.visitType(it) }
    }

    internal open fun generateFieldDeserialization(
        witnessGroupOptions: TransactionComponentOptions,
        field: FieldWithParentStruct,
        witnessIndex: String,
        witnessVariable: String
    ): String {
        val offset = field.generateConstant(OFFSET) + " + $witnessIndex"
        val deserializedField = field.type.deserializeExpr(witnessGroupOptions, offset, field.name, witnessVariable)
        return "let ${field.name}: ${field.type.id} = $deserializedField;"
    }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincInvocable> {
        return listOf(
            generateNewMethod(codeGenerationOptions),
            generateEmptyMethod(codeGenerationOptions),
            generateEqualsMethod(codeGenerationOptions),
        ) + if (isDeserializable) {
            generateDeserializeMethods(codeGenerationOptions)
        } else {
            emptyList()
        } + functions
    }

    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions) = zincFile {
        comment("$id module")
        newLine()
        mod {
            module = CONSTS
        }
        val modulesToImport = getModulesToImport()
        for (mod in modulesToImport) {
            mod { module = mod.getModuleName() }
        }
        if (modulesToImport.isNotEmpty()) {
            newLine()
        }
        for (module in modulesToImport) {
            add(module.use())
            if (isDeserializable && ((module !is BflStruct) || module.isDeserializable)) {
                add(module.useLengthConstant())
            }
            newLine()
        }
        if (isDeserializable) {
            comment("field lengths")
            addAll(generateFieldLengthConstants())
            newLine()
            comment("field offsets")
            addAll(generateFieldOffsetConstants())
            newLine()
            comment("length: ${fields.fold(0) { acc, field -> acc + field.type.bitSize }} bit(s)")
            add(generateLengthConstant())
            newLine()
        }
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
            addMethods(generateMethods(codeGenerationOptions))
            addMethods(getRegisteredMethods())
        }
    }

    internal fun constructSelf(fieldsGenerator: () -> String): String = if (fields.isEmpty()) {
        "Self"
    } else {
        """
            Self {
                ${fieldsGenerator().indent(16.spaces)}
            }
        """.trimIndent()
    }

    open fun generateNewMethod(codeGenerationOptions: CodeGenerationOptions): ZincFunction {
        return zincFunction {
            name = "new"
            for (field in fields) {
                parameter { name = "p_${field.name}"; type = field.type.toZincId() }
            }
            returnType = Self
            comment = """
                Constructs a new $id, from the fields [${fields.joinToString { "p_${it.name}" }}]
            """.trimIndent()
            body = constructSelf {
                fields.joinToString("\n") { "${it.name}: p_${it.name}," }
            }
        }
    }

    open fun generateEmptyMethod(codeGenerationOptions: CodeGenerationOptions): ZincFunction {
        return zincFunction {
            name = "empty"
            returnType = Self
            comment = """
                Constructs a default instance of $id, with all fields set to default values. 
            """.trimIndent()
            body = constructSelf {
                fields.joinToString("\n") {
                    "${it.name}: ${it.type.defaultExpr()},"
                }
            }
        }
    }

    open fun generateFieldEquals(field: FieldWithParentStruct) =
        "(${field.type.equalsExpr("self.${field.name}", "other.${field.name}")})"

    open fun generateEqualsMethod(codeGenerationOptions: CodeGenerationOptions) = zincMethod {
        name = "equals"
        parameter { name = "other"; type = Self }
        returnType = ZincPrimitive.Bool
        comment = """
            Checks whether `self` and `other` are equal.
        """.trimIndent()
        body = if (fields.isNotEmpty()) {
            fields.joinToString("\n&& ") { generateFieldEquals(it) }
        } else {
            "true"
        }
    }

    open fun generateDeserializeMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> {
        return codeGenerationOptions.transactionComponentOptions.map {
            val fieldDeserializations = fields.fold(Pair("", 0)) { acc: Pair<String, Int>, field ->
                val implementation = generateFieldDeserialization(it, field, OFFSET, SERIALIZED)
                Pair(
                    "${acc.first}$implementation\n",
                    acc.second + field.type.bitSize
                )
            }.first
            zincFunction {
                name = it.deserializeMethodName
                parameter { name = SERIALIZED; type = it.witnessType }
                parameter { name = OFFSET; type = ZincPrimitive.U24 }
                returnType = Self
                comment = """
                    Deserialize ${aOrAn()} $id from the ${it.name.capitalize()} group `$SERIALIZED`, at `$OFFSET`.
                """.trimIndent()
                body =
                    fieldDeserializations + "\n" +
                    constructSelf {
                        fields.joinToString("\n") { "${it.name}: ${it.name}," }
                    }
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
        var previous: FieldWithParentStruct? = null
        return fields.map { field ->
            val itsPrevious = previous
            previous = field
            ZincConstant.zincConstant {
                name = field.generateConstant(OFFSET)
                type = ZincPrimitive.U24
                initialization = itsPrevious?.let {
                    """
                        ${it.generateConstant(OFFSET)}
                        + ${it.generateConstant("length")}
                    """.trimIndent()
                } ?: "0 as u24"
            }
        }
    }

    private fun generateLengthConstant(): ZincConstant {
        return fields.lastOrNull()?.let { lastField ->
            ZincConstant.zincConstant {
                name = getLengthConstant()
                type = ZincPrimitive.U24
                initialization = """
                    ${lastField.generateConstant(OFFSET)}
                    + ${lastField.generateConstant("length")}
                """.trimIndent()
            }
        } ?: ZincConstant.zincConstant {
            name = getLengthConstant()
            type = ZincPrimitive.U24
            initialization = "0 as u24"
        }
    }

    internal fun getRecursiveFields(): List<BflStructField> {
        return fields.flatMap { field ->
            val fieldFields = (field.type as? BflStruct)?.let { struct ->
                struct.getRecursiveFields().map {
                    Field("${field.name}.${it.name}", it.type)
                }
            } ?: emptyList()
            fieldFields + field
        }
    }

    override fun toZincType(): ZincStruct = zincStruct {
        name = this@BflStruct.id
        this@BflStruct.fields.forEach {
            field {
                name = it.name
                type = it.type.toZincType()
            }
        }
    }

    override fun toStructureTree(): Tree<BflSized, BflSized> {
        return Tree.node(toNodeDescriptor()) {
            fields.forEach {
                node(NodeDescriptor(it.name, it.type.bitSize)) {
                    addNode(it.type.toStructureTree())
                }
            }
        }
    }
}
