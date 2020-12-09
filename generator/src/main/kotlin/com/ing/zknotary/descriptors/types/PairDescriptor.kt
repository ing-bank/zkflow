package com.ing.zknotary.descriptors.types

import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.CodeBlock

class PairDescriptor(innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(Pair::class, innerDescriptors) {
    /**
     * Index accesses to inner descriptors shall not fail,
     * existence of inner types is verified during the construction.
     */

    override val default: CodeBlock
        get() {
            val (first, second) = innerDescriptors.subList(0, 2)

            return CodeBlock.of(
                "Pair( %L, %L )",
                first.default, second.default
            )
        }

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val (first, second) = innerDescriptors.subList(0, 2)

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
