package com.ing.zknotary.descriptors.types

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zknotary.descriptors.TypeDescriptor
import com.ing.zknotary.util.sizedName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

class AnnotatedSizedClassDescriptor(declaration: KSClassDeclaration) : TypeDescriptor(
    ClassName(
        declaration.packageName.asString(),
        listOf(declaration.sizedName)
    )
) {
    override val type: TypeName
        get() = definition

    override val default: CodeBlock
        get() = CodeBlock.of("%L()", definition)

    override fun toCodeBlock(propertyName: String) =
        CodeBlock.of("%L( %L )", definition, propertyName)
}
