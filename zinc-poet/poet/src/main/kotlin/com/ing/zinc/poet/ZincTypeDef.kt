package com.ing.zinc.poet

/**
 * A [ZincType] that represents a type alias definition, e.g.

 * ```
 * type BitArray16 = [bool; 16];
 * ```
 */
interface ZincTypeDef : ZincType, ZincFileItem {
    /**
     * The alias for the defined type.
     */
    fun getName(): String

    /**
     * The actual type.
     */
    fun getType(): ZincType

    @ZincDslMarker
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
