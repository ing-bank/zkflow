package com.ing.zknotary.descriptors.types

import com.google.devtools.ksp.symbol.KSDeclaration
import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

/**
 * Generic class to represent whole numbers.
 * declaration (KSDeclaration) allows to resolve the correct type.
 */
class PrimitiveTypeDescriptor<T>(value: T, declaration: KSDeclaration) : TypeDescriptor(
    ClassName(
        declaration.packageName.asString(),
        listOf(declaration.simpleName.asString())
    )
) {
    override val isTransient = false

    override val type: TypeName
        get() = definition

    override val default = CodeBlock.of("%L", value)

    override fun toCodeBlock(propertyName: String) =
        CodeBlock.of(propertyName)
}
