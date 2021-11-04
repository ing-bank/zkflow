package com.ing.zinc.poet

interface ZincVariant {
    fun getName(): String
    fun getOrdinal(): Int

    class Builder {
        var name: String? = null
        var ordinal: Int? = null

        fun build(): ZincVariant = ImmutableZincVariant(
            requireNotNull(name) { "Required value `name` is null." },
            requireNotNull(ordinal) { "Required value `ordinal` is null." }
        )
    }

    companion object {
        private data class ImmutableZincVariant(
            private val name: String,
            private val ordinal: Int
        ) : ZincVariant {
            override fun getName(): String = name
            override fun getOrdinal(): Int = ordinal
        }

        fun zincVariant(init: Builder.() -> Unit): ZincVariant = Builder().apply(init).build()
    }
}
