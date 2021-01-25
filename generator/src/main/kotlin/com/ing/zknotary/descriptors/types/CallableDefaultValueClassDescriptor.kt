package com.ing.zknotary.descriptors.types

import com.ing.zknotary.annotations.DefaultValue
import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.full.isSubclassOf

class CallableDefaultValueClassDescriptor(defaultValueClassName: String, returnType: ClassName) :
    TypeDescriptor(returnType) {

    private val defaultValueClass = Class.forName(defaultValueClassName).kotlin

    init {
        require(defaultValueClass.isSubclassOf(DefaultValue::class))
    }

    override val isTransient = false

    override val type: TypeName
        get() = definition

    override val default: CodeBlock
        get() {
            return CodeBlock.builder().apply {
                addStatement("%T().default", defaultValueClass)
            }.build()
        }

    override fun toCodeBlock(propertyName: String) =
        CodeBlock.of("%L", propertyName)
}
