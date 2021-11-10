package com.ing.zinc.poet

interface ZincMod : ZincFileItem {
    fun getModule(): String

    class Builder {
        var module: String? = null

        fun build(): ZincMod {
            return ImmutableZincMod(
                requireNotNull(module) { "Required value `module` is null." }
            )
        }
    }

    companion object {
        private data class ImmutableZincMod(
            private val module: String
        ) : ZincMod {
            override fun getModule(): String = module
            override fun generate(): String = "mod ${getModule()};"
        }

        fun zincMod(init: Builder.() -> Unit): ZincMod = Builder().apply(init).build()
    }
}
