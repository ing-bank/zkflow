package com.ing.zinc.poet

import com.ing.zinc.poet.Indentation.Companion.spaces

interface ZincImpl : ZincFileItem {
    fun getName(): String
    fun getMethods(): List<ZincFunction>

    @ZincDslMarker
    class Builder {
        var name: String? = null
        private val methods: MutableList<ZincFunction> = mutableListOf()

        fun addFunction(function: ZincFunction): Builder {
            this.methods.add(function)
            return this
        }

        fun addFunctions(functions: Collection<ZincFunction>): Builder {
            this.methods.addAll(functions)
            return this
        }

        fun function(init: ZincFunction.Builder.() -> Unit): Builder {
            this.methods.add(ZincFunction.zincFunction(init))
            return this
        }

        fun addMethod(method: ZincMethod): Builder {
            this.methods.add(method)
            return this
        }

        fun addMethods(methods: Collection<ZincMethod>): Builder {
            this.methods.addAll(methods)
            return this
        }

        fun method(init: ZincMethod.Builder.() -> Unit): Builder {
            this.methods.add(ZincMethod.zincMethod(init))
            return this
        }

        fun build(): ZincImpl = ImmutableZincImpl(
            requireNotNull(name) { "Required value `name` is null." },
            methods.toList()
        )
    }

    companion object {
        private data class ImmutableZincImpl(
            private val name: String,
            private val methods: List<ZincFunction>
        ) : ZincImpl {
            override fun getName(): String = name
            override fun getMethods(): List<ZincFunction> = methods
            override fun generate(): String {
                val functions = getMethods().joinToString("\n\n") {
                    it.generate()
                }
                return """
                    impl ${getName()} {
                        ${functions.indent(24.spaces)}
                    }
                """.trimIndent()
            }
        }

        fun zincImpl(init: Builder.() -> Unit): ZincImpl = Builder().apply(init).build()
    }
}
