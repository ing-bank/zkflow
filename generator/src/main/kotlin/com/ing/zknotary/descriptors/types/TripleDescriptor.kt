package com.ing.zknotary.descriptors.types

import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.CodeBlock

class TripleDescriptor(innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(Triple::class, innerDescriptors) {
    /**
     * Index accesses to inner descriptors shall not fail,
     * existence of inner types is verified during the construction.
     */

    override val default: CodeBlock
        get() {
            val (first, second, third) = innerDescriptors.subList(0, 3)

            return CodeBlock.of(
                "Triple( %L, %L, %L )",
                first.default, second.default, third.default
            )
        }

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val (first, second, third) = innerDescriptors.subList(0, 3)

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
