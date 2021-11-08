package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.Self
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincParameter.Companion.zincParameter
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.indent
import java.util.Objects

@Suppress("TooManyFunctions")
open class BflList(
    open val capacity: Int,
    val elementType: BflType,
    id: String,
    val sizeType: BflPrimitive = BflPrimitive.U32
) : BflStruct(
    id,
    listOf(
        Field(SIZE_FIELD, sizeType),
        Field(VALUES_FIELD, BflArray(capacity, elementType))
    )
) {
    constructor(capacity: Int, elementType: BflType) : this(
        capacity,
        elementType,
        "${elementType.typeName()}List$capacity"
    )

    override fun equals(other: Any?) =
        when (other) {
            is BflList -> capacity == other.capacity && elementType == other.elementType
            else -> false
        }

    override fun hashCode() = Objects.hash(capacity, elementType)

    override fun getModulesToImport(): List<BflModule> {
        val recursiveTypes = (elementType as? BflStruct)
            ?.getRecursiveFields()
            ?.flatMap {
                listOf(it.type, BflList(capacity, it.type))
            } ?: emptyList()
        val allTypes = fields.map { it.type } + recursiveTypes
        return allTypes
            .asSequence()
            .flatMap {
                when (it) {
                    is BflArray -> listOf(it.elementType)
                    else -> listOf(it)
                }
            }
            .distinctBy { it.id }
            .filterIsInstance<BflModule>()
            .toList()
    }

    override fun generateFieldDeserialization(
        witnessGroupOptions: WitnessGroupOptions,
        field: FieldWithParentStruct,
        witnessIndex: String
    ): String {
        return if (field.name == VALUES_FIELD) {
            val offset = field.generateConstant(OFFSET) + " + $witnessIndex"
            val variablePrefix = field.name
            val array = "${variablePrefix}_array"
            val i = "${variablePrefix}_i"
            val deserializedItem = elementType.deserializeExpr(
                witnessGroupOptions,
                offset = "$i as u24 * ${elementType.sizeExpr()} + $offset",
                variablePrefix = array
            )
            val deserializedField = """
            {
                let mut $array: [${elementType.id}; $capacity] = [${elementType.defaultExpr()}; $capacity];
                for $i in (0 as ${sizeType.id})..$capacity while $i < $SIZE_FIELD {
                    $array[$i] = ${deserializedItem.indent(20.spaces)};
                }
                $array
            }
            """.trimIndent()
            "let ${field.name}: ${field.type.id} = $deserializedField;"
        } else {
            super.generateFieldDeserialization(witnessGroupOptions, field, witnessIndex)
        }
    }

    override fun generateFieldEquals(field: FieldWithParentStruct): String {
        return if (field.name == VALUES_FIELD) {
            val selfValues = "self.${field.name}"
            val otherValues = "other.${field.name}"
            val selfSize = "self.$SIZE_FIELD"
            val prefix = selfValues
                .replace("self.", "")
                .replace("[.\\[\\]]".toRegex(), "_")
            val i = "${prefix}_i"
            val stillEquals = "${prefix}_still_equals"
            val elementEquals = elementType.equalsExpr("$selfValues[$i]", "$otherValues[$i]")
            """
            {
                let mut $stillEquals: bool = true;
                for $i in (0 as ${sizeType.id})..$capacity while $stillEquals && $i < $selfSize {
                    $stillEquals = ${elementEquals.indent(20.spaces)};
                }
                $stillEquals
            }
            """.trimIndent()
        } else {
            super.generateFieldEquals(field)
        }
    }

    @ZincMethod(order = 10)
    @Suppress("unused")
    open fun generateListOfMethod(codeGenerationOptions: CodeGenerationOptions): ZincFunction {
        val fieldAssignments = fields.joinToString("\n") {
            when (it.name) {
                SIZE_FIELD -> "${it.name}: $capacity as ${sizeType.id},"
                else -> "${it.name}: ${it.name},"
            }
        }
        return zincFunction {
            name = "list_of"
            addParameters(
                fields
                    .filterNot { it.name == SIZE_FIELD }
                    .map { zincParameter { name = it.name; type = it.type.toZincId() } }
            )
            returnType = Self
            comment = "Construct a list with given data, and set the `size` to the full capacity."
            body = """
                Self {
                    ${fieldAssignments.indent(20.spaces)}
                }
            """.trimIndent()
        }
    }

    @ZincMethod(order = 45)
    @Suppress("unused")
    internal fun generateContainsMethod(): ZincFunction = zincMethod {
        name = "contains"
        parameter { name = ELEMENT; type = elementType.toZincId() }
        returnType = ZincPrimitive.Bool
        comment = "Checks whether `self` contains the given `element`."
        body = """
            let mut contains_element: bool = false;
            for i in (0 as ${sizeType.id})..$capacity while !contains_element && i < self.$SIZE_FIELD {
                contains_element = ${elementType.equalsExpr("self.$VALUES_FIELD[i]", ELEMENT).indent(16.spaces)};
            }
            contains_element
        """.trimIndent()
    }

    @ZincMethod(order = 40)
    @Suppress("unused")
    open fun generateGetMethod(codeGenerationOptions: CodeGenerationOptions): ZincFunction = zincMethod {
        name = "get"
        parameter { name = "index"; type = sizeType.toZincId() }
        returnType = elementType.toZincId()
        comment = "Return element at `index`."
        body = """
            assert!(index < self.$SIZE_FIELD, "Index out of bounds!");
            self.$VALUES_FIELD[index]
        """.trimIndent()
    }

    @ZincMethod(order = 50)
    @Suppress("unused")
    internal fun generateIsSubsetOfMethod(): ZincFunction = zincMethod {
        name = "is_subset_of"
        parameter { name = "other"; type = Self }
        returnType = ZincPrimitive.Bool
        comment = "Checks whether `self` is a subset of `other`."
        body = """
            let mut still_subset: bool = true;
            for i in (0 as ${sizeType.id})..$capacity while still_subset && i < self.$SIZE_FIELD {
                still_subset = other.contains(self.get(i));
            }
            still_subset
        """.trimIndent()
    }

    /**
     * TODO the add method does not work when the value isn't returned, so not really mutable?
     */
    @ZincMethod(order = 65)
    @Suppress("unused")
    internal fun generateAddMethod(): ZincFunction = zincMethod {
        mutable = true
        name = "add"
        parameter { name = ELEMENT; type = elementType.toZincId() }
        returnType = Self
        comment = """
            Add `element` to `self`.
            Panics when there is no capacity.
        """.trimIndent()
        body = """
            assert!(self.$SIZE_FIELD < $capacity as ${sizeType.id}, "List out of capacity!");
            self.$VALUES_FIELD[self.$SIZE_FIELD] = element;
            self.$SIZE_FIELD = self.$SIZE_FIELD + 1 as ${sizeType.id};
            self
        """.trimIndent()
    }

    /**
     * This implementation is O(n^2).
     */
    @ZincMethod(order = 70)
    @Suppress("unused")
    internal fun generateIsDistinctMethod(): ZincFunction = zincMethod {
        name = "is_distinct"
        returnType = ZincPrimitive.Bool
        comment = "Checks whether all elements in `self` are distinct."
        body = """
            let mut still_distinct: ${BflPrimitive.Bool.id} = true;
            for i in (0 as ${sizeType.id})..$capacity while i < self.$SIZE_FIELD && still_distinct {
                let element = self.get(i);
                for j in (0 as ${sizeType.id})..$capacity while j < i && still_distinct {
                    still_distinct = !(${elementType.equalsExpr("self.$VALUES_FIELD[i]", "self.$VALUES_FIELD[j]").indent(20.spaces)});
                }
            }
            still_distinct
        """.trimIndent()
    }

    @ZincMethod(order = 72)
    @Suppress("unused")
    internal fun generateAllEqualsMethod(): ZincFunction {
        val equalsExpr = elementType.equalsExpr("self.$VALUES_FIELD[i]", ELEMENT)
        return zincMethod {
            name = "all_equals"
            parameter { name = ELEMENT; type = elementType.toZincId() }
            returnType = ZincPrimitive.Bool
            comment = """
                Checks whether all elements in `self` are equal to `element`.
                Pseudo code:
                
                    self.all(|field| -> { field.equals(element) })
            """.trimIndent()
            body = """
                let mut still_equals = true;
                for i in (0 as ${sizeType.id})..$capacity while still_equals && i < self.$SIZE_FIELD {
                    still_equals = ${equalsExpr.indent(20.spaces)};
                }
                still_equals
            """.trimIndent()
        }
    }

    @ZincMethodList(order = 60)
    @Suppress("unused")
    internal fun generateExtractFieldMethods(): List<ZincFunction> {
        return (elementType as? BflStruct)
            ?.getRecursiveFields()
            ?.map(this@BflList::generateExtractFieldMethod)
            ?: emptyList()
    }

    private fun generateExtractFieldMethod(
        field: BflStructField,
    ): ZincFunction {
        val fieldListType = BflList(capacity, field.type)
        return zincMethod {
            name = "extract_".withFieldSuffix(field)
            returnType = fieldListType.toZincId()
            comment = """
                Returns a new List with the `${field.name}` field of each element.
                Pseudo code:
                
                   self.map(|field| -> { field.${field.name} })
            """.trimIndent()
            body = """
                let mut result: ${fieldListType.id} = ${fieldListType.defaultExpr()};
                for i in (0 as ${sizeType.id})..$capacity while i < self.$SIZE_FIELD {
                    result.$VALUES_FIELD[i] = self.$VALUES_FIELD[i].${field.name};
                }
                result.$SIZE_FIELD = self.$SIZE_FIELD;
                result
            """.trimIndent()
        }
    }

    @ZincMethodList(order = 80)
    @Suppress("unused")
    internal fun generateIndexOfSingleByFieldMethods(): List<ZincFunction> {
        return (elementType as? BflStruct)?.getRecursiveFields()?.map(this@BflList::generateIndexOfSingleByFieldMethod)
            ?: emptyList()
    }

    private fun generateIndexOfSingleByFieldMethod(
        field: BflStructField,
    ): ZincFunction {
        val equalsExpr = field.type.equalsExpr("self.$VALUES_FIELD[i].${field.name}", "by")
        return zincMethod {
            name = "index_of_single_by_".withFieldSuffix(field)
            parameter { name = "by"; type = field.type.toZincId() }
            returnType = sizeType.toZincId()
            comment = """
                Returns the index of a single element matching `by`.
                This method panics when there are zero or multiple matches.
                Pseudo code:
                
                   self.indexOfSingle(|field| -> { field.${field.name} == by })
            """.trimIndent()
            body = """
                let mut found: bool = false;
                let mut result: ${sizeType.id} = ${sizeType.defaultExpr()};
                for i in (0 as ${sizeType.id})..$capacity while i < self.$SIZE_FIELD {
                    if ${equalsExpr.indent(20.spaces)} {
                        assert!(!found, "Multiple matches found for field ${field.name}");
                        result = i;
                        found = true
                    }
                }
                assert!(found, "No match found for field ${field.name}");
                result
            """.trimIndent()
        }
    }

    @ZincMethodList(order = 81)
    @Suppress("unused")
    internal fun generateSingleByFieldMethods(): List<ZincFunction> {
        return (elementType as? BflStruct)?.run {
            getRecursiveFields().map { field ->
                val equalsExpr = field.type.equalsExpr("self.$VALUES_FIELD[i].${field.name}", "by")
                zincMethod {
                    name = "single_by_".withFieldSuffix(field)
                    parameter { name = "by"; type = field.type.toZincId() }
                    returnType = elementType.toZincId()
                    comment = """
                        Returns a single element matching `by`.
                        This method panics when there are zero or multiple matches.
                        Pseudo code:
                        
                           self.single(|field| -> { field.${field.name} == by })
                    """.trimIndent()
                    body = """
                        let mut found: bool = false;
                        let mut result: ${elementType.id} = ${elementType.defaultExpr()};
                        for i in (0 as ${sizeType.id})..$capacity while i < self.$SIZE_FIELD {
                            if ${equalsExpr.indent(28.spaces)} {
                                assert!(!found, "Multiple matches found for field ${field.name}");
                                result = self.$VALUES_FIELD[i];
                                found = true
                            }
                        }
                        assert!(found, "No match found for field ${field.name}");
                        result
                    """.trimIndent()
                }
            }
        } ?: emptyList()
    }

    override fun accept(visitor: TypeVisitor) {
        super.accept(visitor)
        /**
         * When the [elementType] is a [BflStruct], visit list types for all struct fields, as they are used in
         * [generateExtractFieldMethods]
         */
        if (elementType is BflStruct) {
            elementType.getRecursiveFields().forEach { field ->
                val fieldListType = BflList(capacity, field.type)
                visitor.visitType(fieldListType)
            }
        }
    }

    companion object {
        const val SIZE_FIELD = "size"
        const val VALUES_FIELD = "values"
        const val ELEMENT = "element"

        private fun String.withFieldSuffix(field: BflStructField): String {
            return this + field.name.replace(".", "_")
        }
    }
}
