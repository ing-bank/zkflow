package com.ing.zknotary.descriptors.types

import com.ing.zknotary.annotations.SizedList
import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.CodeBlock

class ListDescriptor(private val size: Int, innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(
    SizedList::class,
    innerDescriptors
) {
    /**
     * Index accesses to inner descriptors shall not fail,
     * existence of inner types is verified during the construction.
     */

    override val default: CodeBlock
        get() {
            val inner = innerDescriptors.first()

            return CodeBlock.of(
                "SizedList( %L, %L )",
                size, inner.default
            )
        }

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val inner = innerDescriptors.first()

        val map = mutableMapOf(
            "propertyName" to propertyName,
            "size" to size,
            "default" to inner.default
        )
        var mapper = ""

        if (inner.isTransient) {
            val itName = "it${(0..1000).random()}"
            map += "it" to itName
            map += "mapped" to inner.toCodeBlock(itName)

            mapper = ".map { %it:L ->\n⇥%mapped:L\n⇤}"
        }

        return CodeBlock.builder()
            .addNamed(
                "SizedList.fromIterator(" +
                    "\n⇥iterator = %propertyName:L$mapper.iterator()," +
                    "\nn = %size:L," +
                    "\ndefault = %default:L\n⇤)",
                map
            ).build()
    }
}
