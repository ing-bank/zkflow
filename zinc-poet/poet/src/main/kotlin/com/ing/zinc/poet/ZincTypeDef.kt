package com.ing.zinc.poet

interface ZincTypeDef : ZincType, ZincFileItem {
    fun getName(): String
    fun getType(): ZincType

    class Builder {
        var name: String? = null
        var type: ZincType? = null

        fun build(): ZincTypeDef = ImmutableZincTypeDef(
            requireNotNull(name) { "Required value `name` is null." },
            requireNotNull(type) { "Required value `type` is null." }
        )
    }

    companion object {
        private data class ImmutableZincTypeDef(
            private val name: String,
            private val type: ZincType
        ) : ZincTypeDef {
            override fun getId(): String = name
            override fun getName(): String = name
            override fun getType(): ZincType = type
            override fun generate(): String = "type ${getName()} = ${getType().getId()};"
        }

        fun zincTypeDef(init: Builder.() -> Unit): ZincTypeDef = Builder().apply(init).build()
    }
}
