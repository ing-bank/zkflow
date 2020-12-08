package com.ing.zknotary.descriptors.types

import com.ing.zknotary.annotations.SizedList
import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.CodeBlock

class ListDescriptor(private val size: Int, innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(
    SizedList::class,
    innerDescriptors
) {
    override val default: CodeBlock
        get() {
            val innerType = innerDescriptors.getOrNull(0)
                ?: error("SizedList must declare type of its elements")

            return CodeBlock.of(
                "SizedList( %L, %L )",
                size, innerType.default
            )
        }

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val listType = innerDescriptors.getOrNull(0)
            ?: error("SizedList must declare type of its elements")

        val map = mutableMapOf(
            "propertyName" to propertyName,
            "size" to size,
            "default" to listType.default
        )
        var mapper = ""

        if (listType.isTransient) {
            val itName = "it${(0..1000).random()}"
            map += "it" to itName
            map += "mapped" to listType.toCodeBlock(itName)

            mapper = ".map { %it:L ->\n⇥%mapped:L\n⇤}"
        }

        return CodeBlock.builder()
            .addNamed(
                "SizedList(" +
                    "\n⇥n = %size:L," +
                    "\nlist = %propertyName:L$mapper," +
                    "\ndefault = %default:L\n⇤)",
                map
            ).build()
    }
}
