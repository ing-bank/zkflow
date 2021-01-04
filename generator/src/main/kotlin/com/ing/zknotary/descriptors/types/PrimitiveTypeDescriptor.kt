package com.ing.zknotary.descriptors.types

import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

/**
 * Generic class to represent whole numbers.
 */
class PrimitiveTypeDescriptor<T>(val value: T, className: ClassName) : TypeDescriptor(className) {
    override val isTransient = false

    override val type: TypeName
        get() = definition

    override val default = CodeBlock.of("%L", value)

    override fun toCodeBlock(propertyName: String) =
        CodeBlock.of(propertyName)
}
