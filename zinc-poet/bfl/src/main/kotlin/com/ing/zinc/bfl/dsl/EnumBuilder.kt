package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflEnum
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.requireNotEmpty
import kotlin.reflect.KClass

class EnumBuilder {
    var id: String? = null
    private val variants: MutableList<String> = mutableListOf()
    private val methods: MutableList<ZincFunction> = mutableListOf()

    fun variant(variant: String) {
        variants.add(variant)
    }

    fun addVariants(variants: Collection<String>) {
        this.variants.addAll(variants)
    }

    fun method(init: ZincMethod.Builder.() -> Unit) {
        this.methods.add(ZincMethod.zincMethod(init))
    }

    fun function(init: ZincFunction.Builder.() -> Unit) {
        this.methods.add(ZincFunction.zincFunction(init))
    }

    fun build() = BflEnum(
        requireNotNull(id) { "Enum property id is missing" },
        requireNotEmpty(variants) { "Enum must have at least 1 field" },
        methods
    )

    companion object {
        fun enum(init: EnumBuilder.() -> Unit): BflEnum = EnumBuilder().apply(init).build()
        fun enumOf(enum: KClass<out Enum<*>>, init: EnumBuilder.() -> Unit = {}): BflEnum {
            return EnumBuilder().apply {
                id = enum.simpleName
                addVariants(enum.java.enumConstants.map { it.name }.toList())
                apply(init)
            }.build()
        }
    }
}
