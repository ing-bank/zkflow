package com.ing.zinc.poet

import com.ing.zinc.poet.Indentation.Companion.spaces

interface ZincEnum : ZincType, ZincFileItem {
    fun getName(): String
    fun getVariants(): List<ZincVariant>

    @ZincDslMarker
    class Builder {
        var name: String? = null
        private val variants: MutableList<ZincVariant> = mutableListOf()

        fun addVariant(variant: ZincVariant): Builder {
            this.variants.add(variant)
            return this
        }

        fun addVariants(variants: List<ZincVariant>): Builder {
            this.variants.addAll(variants)
            return this
        }

        fun variant(init: ZincVariant.Builder.() -> Unit): Builder {
            this.variants.add(ZincVariant.zincVariant(init))
            return this
        }

        fun build(): ZincEnum = ImmutableZincEnum(
            requireNotNull(name) { "Required value `name` is null." },
            variants.toList()
        )
    }

    companion object {
        private data class ImmutableZincEnum(
            private val name: String,
            private val variants: List<ZincVariant>
        ) : ZincEnum {
            override fun getId(): String = name
            override fun getName(): String = name
            override fun getVariants(): List<ZincVariant> = variants
            override fun generate(): String {
                val variantString = getVariants().joinToString("\n") {
                    "${it.getName()} = ${it.getOrdinal()},"
                }
                return """
                    enum ${getName()} {
                        ${variantString.indent(24.spaces)}
                    }
                """.trimIndent()
            }
        }

        fun zincEnum(init: Builder.() -> Unit): ZincEnum = Builder().apply(init).build()
    }
}
