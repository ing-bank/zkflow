package com.ing.zknotary.descriptors.types

import com.ing.zknotary.annotations.DefaultValue
import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.full.isSubclassOf

class CallableKClassDescriptor(private val usedImports: List<String>, private val defaultValueClassName: String, returnType: ClassName) :
    TypeDescriptor(returnType) {

    private val defaultValueClass = Class.forName(defaultValueClassName)

    init {
        defaultValueClass.kotlin.isSubclassOf(DefaultValue::class)
    }

    override val isTransient = false

    override val type: TypeName
        get() = definition

    override val ownImports = usedImports.map { Class.forName(it).asClassName() }

    override val default: CodeBlock
        get() {
            return CodeBlock.builder().apply {
                addStatement("%L().default", defaultValueClassName)
            }.build()
        }

    override fun toCodeBlock(propertyName: String) =
        CodeBlock.of("%L", propertyName)
}
