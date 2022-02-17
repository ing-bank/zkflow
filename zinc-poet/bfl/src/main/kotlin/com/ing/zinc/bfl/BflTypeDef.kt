package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincTypeDef

class BflTypeDef(
    override val id: String,
    private val typeDef: BflType,
) : BflModule {
    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile = zincFile {
        if (typeDef is BflModule) {
            mod { module = typeDef.getModuleName() }
            use { path = "${typeDef.getModuleName()}::${typeDef.id}" }
            use { path = "${typeDef.getModuleName()}::${typeDef.getSerializedTypeDef().getName()}" }
            use { path = "${typeDef.getModuleName()}::${typeDef.getLengthConstant()}" }
            newLine()
            constant {
                name = getLengthConstant()
                type = ZincPrimitive.U24
                initialization = typeDef.getLengthConstant()
            }
            newLine()
        } else {
            constant {
                name = getLengthConstant()
                type = ZincPrimitive.U24
                initialization = "${typeDef.bitSize} as u24"
            }
        }
        add(getSerializedTypeDef())
        newLine()
        type {
            name = id
            type = typeDef.toZincId()
        }
    }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> = emptyList()

    override val bitSize: Int = typeDef.bitSize

    override fun typeName(): String = id

    override fun deserializeExpr(
        witnessGroupOptions: WitnessGroupOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String = typeDef.deserializeExpr(witnessGroupOptions, offset, variablePrefix, witnessVariable)

    override fun defaultExpr(): String = typeDef.defaultExpr()

    override fun equalsExpr(self: String, other: String): String = typeDef.equalsExpr(self, other)

    override fun accept(visitor: TypeVisitor) {
        visitor.visitType(typeDef)
    }

    override fun toZincType(): ZincTypeDef = ZincTypeDef.zincTypeDef {
        name = this@BflTypeDef.typeName()
        type = this@BflTypeDef.typeDef.toZincType()
    }
}
