package com.ing.zknotary.descriptors.types

import com.google.devtools.ksp.symbol.KSDeclaration
import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

// it's impossible to get to the package name for `Int` from `Int::class`
// 1. Kotlin has not such native functionality,
//    although I can resolve it from `Int::class.qualifiedName` (evaluates to `kotlin.Int`),
//    but I don't want to hardcode such a thing.
// 2. `KSDeclaration` allows to resolve package name, so I prefer
//    taking an extra parameter providing the required functionality.
class Int_(val value: Int, declaration: KSDeclaration) : TypeDescriptor(
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