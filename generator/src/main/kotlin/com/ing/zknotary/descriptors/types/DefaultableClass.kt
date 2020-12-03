package com.ing.zknotary.descriptors.types

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zknotary.descriptors.TypeDescriptor
import com.ing.zknotary.util.toClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

class DefaultableClass(declaration: KSClassDeclaration) : TypeDescriptor(
    declaration.toClassName()
) {
    override val isTransient = false

    override val type: TypeName
        get() = definition

    override val default: CodeBlock
        get() = CodeBlock.of("%L()", definition)

    override fun toCodeBlock(propertyName: String) =
        CodeBlock.of("%L", propertyName)
}
