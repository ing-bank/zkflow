package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.poet.ZincConstant
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.util.BflSized
import com.ing.zkflow.util.Tree

@Suppress("TooManyFunctions")
object BflUnit : BflModule {
    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile {
        return zincFile {
            type {
                name = typeName()
                type = toZincType()
            }
            add(generateLengthConstant())
            add(getSerializedTypeDef())
        }
    }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> = emptyList()

    override val id: String = "()"

    override val bitSize: Int = 0

    override fun typeName(): String = "Unit"

    override fun deserializeExpr(
        transactionComponentOptions: TransactionComponentOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String = defaultExpr()

    override fun defaultExpr(): String = "()"

    override fun equalsExpr(self: String, other: String): String = "true"

    override fun accept(visitor: TypeVisitor) {
        // NOOP
    }

    override fun toZincType(): ZincType {
        return ZincPrimitive.Unit
    }

    override fun getModuleName(): String {
        return "unit"
    }

    private fun generateLengthConstant(): ZincConstant = ZincConstant.zincConstant {
        name = getLengthConstant()
        type = ZincPrimitive.U24
        initialization = "0 as u24"
    }

    override fun toString(): String = id

    override fun toStructureTree(): Tree<BflSized, BflSized> {
        return Tree.leaf(toNodeDescriptor())
    }
}
