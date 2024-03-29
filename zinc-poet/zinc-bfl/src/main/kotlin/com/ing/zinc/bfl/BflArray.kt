package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.indent
import com.ing.zkflow.util.BflSized
import com.ing.zkflow.util.Tree

data class BflArray(
    val arraySize: Int,
    val elementType: BflType
) : BflType {
    override val id = "[${elementType.id}; $arraySize]"

    override val bitSize = arraySize * elementType.bitSize

    override fun typeName() = "${elementType.typeName()}Array$arraySize"
    override fun deserializeExpr(
        transactionComponentOptions: TransactionComponentOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String {
        val array = "${variablePrefix}_array"
        val i = "${variablePrefix}_i"
        val o = "${variablePrefix}_offset"
        val deserializeExpr = elementType.deserializeExpr(
            transactionComponentOptions,
            offset = "$i * ${elementType.bitSize} as u24 + $o",
            variablePrefix = array,
            witnessVariable = witnessVariable
        )
        return """
            {
                let $o: u24 = $offset;
                let mut $array: [${elementType.id}; $arraySize] = [${elementType.defaultExpr()}; $arraySize];
                for $i in (0 as u24)..$arraySize {
                    $array[$i] = ${deserializeExpr.indent(20.spaces)};
                }
                $array
            }
        """.trimIndent()
    }

    override fun defaultExpr(): String {
        return "[${elementType.defaultExpr()}; $arraySize]"
    }

    override fun equalsExpr(self: String, other: String): String {
        val prefix = self
            .replace("self.", "")
            .replace("[.\\[\\]]".toRegex(), "_")
        val elementEquals = elementType.equalsExpr("$self[${prefix}_i]", "$other[${prefix}_i]")
        return """
            {
                let mut ${prefix}_still_equals: bool = true;
                for ${prefix}_i in (0 as u24)..$arraySize while ${prefix}_still_equals {
                    ${prefix}_still_equals = ${elementEquals.indent(20.spaces)};
                }
                ${prefix}_still_equals
            }
        """.trimIndent()
    }

    override fun sizeExpr(): String {
        return "$arraySize as u24 * ${elementType.sizeExpr()}"
    }

    override fun accept(visitor: TypeVisitor) {
        visitor.visitType(elementType)
    }

    override fun toZincType() = zincArray {
        elementType = this@BflArray.elementType.toZincId()
        size = "${this@BflArray.arraySize}"
    }

    override fun toStructureTree(): Tree<BflSized, BflSized> {
        return Tree.node(toNodeDescriptor()) {
            addNode(elementType.toStructureTree())
        }
    }
}
