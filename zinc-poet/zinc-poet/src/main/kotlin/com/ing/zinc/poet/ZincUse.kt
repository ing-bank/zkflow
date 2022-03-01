package com.ing.zinc.poet

interface ZincUse : ZincFileItem {
    fun getPath(): String

    @ZincDslMarker
    class Builder {
        var path: String? = null

        fun build(): ZincUse {
            return ImmutableZincUse(
                requireNotNull(path) { "Required value `path` is null." }
            )
        }
    }

    companion object {
        private data class ImmutableZincUse(
            private val path: String
        ) : ZincUse {
            override fun getPath(): String = path
            override fun generate(): String = "use ${getPath()};"
        }

        fun zincUse(init: Builder.() -> Unit): ZincUse = Builder().apply(init).build()
    }
}
