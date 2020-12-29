package com.ing.zknotary.descriptors.types

import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

class CallableClassDescriptor(private val usedImports: List<String>, private val code: String, returnType: ClassName) :
    TypeDescriptor(returnType) {
    override val isTransient = false

    override val type: TypeName
        get() = definition

    override val ownImports = usedImports.map { Class.forName(it).asClassName() }

    override val default: CodeBlock
        get() {
            val lambdaType = LambdaTypeName.get(returnType = type)
            val lambdaSpec = PropertySpec.builder("lambda", lambdaType)
                .initializer(CodeBlock.of("{\n⇥%L\n⇤}", code))
                .build()

            val objectSpec = TypeSpec.anonymousClassBuilder()
                .addProperty(lambdaSpec)
                .build()

            val callSpec = CodeBlock.builder().apply {
                addStatement("%L.lambda()", objectSpec)
            }.build()

            return callSpec
        }

    override fun toCodeBlock(propertyName: String) =
        CodeBlock.of("%L", propertyName)
}
