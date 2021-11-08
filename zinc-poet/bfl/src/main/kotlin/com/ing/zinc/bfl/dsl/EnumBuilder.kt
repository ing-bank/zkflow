package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflEnum
import kotlin.reflect.KClass

class EnumBuilder {
    var name: String? = null
    private val variants: MutableList<String> = mutableListOf()

    fun variant(variant: String) {
        variants.add(variant)
    }

    fun addVariants(variants: Collection<String>) {
        this.variants.addAll(variants)
    }

    fun build() = BflEnum(
        requireNotNull(name) { "Enum property name is missing" },
        variants
    )

    companion object {
        fun enum(init: EnumBuilder.() -> Unit): BflEnum = EnumBuilder().apply(init).build()
        fun enumOf(enum: KClass<out Enum<*>>, init: EnumBuilder.() -> Unit = {}): BflEnum {
            return EnumBuilder().apply {
                name = enum.simpleName
                addVariants(enum.java.enumConstants.map { it.name }.toList())
                apply(init)
            }.build()
        }
    }
}
