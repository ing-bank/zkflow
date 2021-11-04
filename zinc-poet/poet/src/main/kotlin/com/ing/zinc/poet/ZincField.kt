package com.ing.zinc.poet

interface ZincField {
    fun getName(): String
    fun getType(): ZincType

    class Builder {
        var name: String? = null
        var type: ZincType? = null

        fun build(): ZincField = ImmutableZincField(
            requireNotNull(name) { "Required value `name` is null." },
            requireNotNull(type) { "Required value `type` is null." }
        )
    }

    companion object {
        private data class ImmutableZincField(
            private val name: String,
            private val type: ZincType
        ) : ZincField {
            override fun getName(): String = name
            override fun getType(): ZincType = type
        }

        fun zincField(init: Builder.() -> Unit): ZincField = Builder().apply(init).build()
    }
}
