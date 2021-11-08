package com.ing.zinc.bfl

import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.indent

data class BflMap(
    override val capacity: Int,
    val keyType: BflType,
    val valueType: BflType
) : BflList(
    capacity,
    BflMapEntry(keyType, valueType),
    "${keyType.typeName()}To${valueType.typeName()}Map$capacity"
) {
    internal val tryGetReturnType = BflOption(valueType)

    override fun getModulesToImport(): List<BflModule> {
        return (super.getModulesToImport() + tryGetReturnType)
            .distinctBy { it.id }
    }

    @ZincMethod(order = 50)
    @Suppress("unused")
    fun generateTryGetMethod(): ZincFunction {
        val mapEntryKey = "self.$VALUES_FIELD[i].${BflMapEntry.KEY_FIELD}"
        val mapEntryValue = "self.$VALUES_FIELD[i].${BflMapEntry.VALUE_FIELD}"
        return zincMethod {
            name = "try_get"
            parameter { name = BflMapEntry.KEY_FIELD; type = keyType.toZincId() }
            returnType = tryGetReturnType.toZincId()
            comment = """
                Try to retrieve an element from `self` by `${BflMapEntry.KEY_FIELD}`.
            """.trimIndent()
            body = """
                let mut result: ${tryGetReturnType.id} = ${tryGetReturnType.defaultExpr()};
                for i in (0 as ${sizeType.id})..$capacity while i < self.$SIZE_FIELD {
                    if ${keyType.equalsExpr(mapEntryKey, BflMapEntry.KEY_FIELD).indent(22.spaces)} {
                        result = ${tryGetReturnType.generateSome(mapEntryValue).indent(24.spaces)};
                    }
                }
                result
            """.trimIndent()
        }
    }

    override fun accept(visitor: TypeVisitor) {
        super.accept(visitor)
        visitor.visitType(tryGetReturnType)
    }
}
