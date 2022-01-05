package com.ing.zinc.poet

interface ZincParameter {
    fun getName(): String
    fun getType(): ZincType
    fun isMutable(): Boolean

    @ZincDslMarker
    class Builder {
        var name: String? = null
        var type: ZincType? = null
        var mutable: Boolean = false

        fun build(): ZincParameter = ImmutableZincParameter(
            requireNotNull(name) { "Required value `name` is null." },
            requireNotNull(type) { "Required value `type` is null." },
            mutable
        )
    }

    companion object {
        private data class ImmutableZincParameter(
            private val name: String,
            private val type: ZincType,
            private val mutable: Boolean
        ) : ZincParameter {
            override fun getName(): String = name
            override fun getType(): ZincType = type
            override fun isMutable(): Boolean = mutable
        }

        fun zincParameter(init: Builder.() -> Unit): ZincParameter = Builder().apply(init).build()
    }
}
