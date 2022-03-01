package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincTypeDef

/**
 * [BflTypeDef] is a [BflType] that represents a [BflModule] for type definitions.
 *
 * This type can be used to `upgrade` a [BflType] which is not a [BflModule] to a [BflModule].
 *
 * The generated module will contain this type definition, and also a type definition for the serialized data. All
 * other methods are delegated to the [typeDeclaration].
 */
data class BflTypeDef(
    override val id: String,
    private val typeDeclaration: BflType,
) : BflModule {
    override fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile = zincFile {
        if (typeDeclaration is BflModule) {
            add(typeDeclaration.mod())
            add(typeDeclaration.use())
            add(typeDeclaration.useSerialized())
            add(typeDeclaration.useLengthConstant())
            newLine()
            constant {
                name = getLengthConstant()
                type = ZincPrimitive.U24
                initialization = typeDeclaration.getLengthConstant()
            }
            newLine()
        } else {
            constant {
                name = getLengthConstant()
                type = ZincPrimitive.U24
                initialization = "${typeDeclaration.bitSize} as u24"
            }
        }
        add(getSerializedTypeDef())
        newLine()
        type {
            name = id
            type = typeDeclaration.toZincId()
        }
    }

    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction> = emptyList()

    override val bitSize: Int = typeDeclaration.bitSize

    override fun typeName(): String = id

    override fun deserializeExpr(
        witnessGroupOptions: WitnessGroupOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String = typeDeclaration.deserializeExpr(witnessGroupOptions, offset, variablePrefix, witnessVariable)

    override fun defaultExpr(): String = typeDeclaration.defaultExpr()

    override fun equalsExpr(self: String, other: String): String = typeDeclaration.equalsExpr(self, other)

    override fun accept(visitor: TypeVisitor) {
        visitor.visitType(typeDeclaration)
    }

    override fun toZincType(): ZincTypeDef = ZincTypeDef.zincTypeDef {
        name = this@BflTypeDef.typeName()
        type = this@BflTypeDef.typeDeclaration.toZincType()
    }
}
