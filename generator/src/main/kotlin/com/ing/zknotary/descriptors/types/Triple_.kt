package com.ing.zknotary.descriptors.types

import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.CodeBlock

class Triple_(innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(Triple::class, innerDescriptors) {
    override val default: CodeBlock
        get() {
            val first = innerDescriptors.getOrNull(0)
                ?: error("Triple<A, B, C> must declare type A")

            val second = innerDescriptors.getOrNull(1)
                ?: error("Triple<A, B, C> must declare type B")

            val third = innerDescriptors.getOrNull(2)
                ?: error("Triple<A, B, C> must declare type C")

            return CodeBlock.of(
                "Triple( %L, %L, %L )",
                first.default, second.default, third.default
            )
        }

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val first = innerDescriptors.getOrNull(0)
            ?: error("Triple<A, B, C> must declare type A")

        val second = innerDescriptors.getOrNull(1)
            ?: error("Triple<A, B, C> must declare type B")

        val third = innerDescriptors.getOrNull(2)
            ?: error("Triple<A, B, C> must declare type C")

        val map = mapOf(
            "propertyName" to propertyName,
            "first" to first.toCodeBlock("$propertyName.first"),
            "second" to second.toCodeBlock("$propertyName.second"),
            "third" to third.toCodeBlock("$propertyName.third")
        )

        return CodeBlock.builder()
            .addNamed(
                "Triple( %first:L, %second:L, %third:L )",
                map
            ).build()
    }
}