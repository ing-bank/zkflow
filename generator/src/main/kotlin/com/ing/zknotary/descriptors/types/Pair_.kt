package com.ing.zknotary.descriptors.types

import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.CodeBlock

class Pair_(innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(Pair::class, innerDescriptors) {
    override val default: CodeBlock
        get() {
            val firstType = innerDescriptors.getOrNull(0)
                ?: error("Pair<A, B> must declare type A")

            val secondType = innerDescriptors.getOrNull(1)
                ?: error("Pair<A, B> must declare type B")

            return CodeBlock.of(
                "Pair( %L, %L )",
                firstType.default, secondType.default
            )
        }

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val first = innerDescriptors.getOrNull(0)
            ?: error("Pair<A, B> must declare type A")
        val second = innerDescriptors.getOrNull(1)
            ?: error("Pair<A, B> must declare type B")

        val map = mapOf(
            "propertyName" to propertyName,
            "first" to first.toCodeBlock("$propertyName.first"),
            "second" to second.toCodeBlock("$propertyName.second")
        )

        return CodeBlock.builder()
            .addNamed(
                "Pair( %first:L, %second:L )",
                map
            ).build()
    }
}