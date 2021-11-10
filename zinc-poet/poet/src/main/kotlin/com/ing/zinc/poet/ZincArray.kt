package com.ing.zinc.poet

interface ZincArray : ZincType {
    fun getElementType(): ZincType
    fun getSize(): String

    class Builder {
        var elementType: ZincType? = null
        var size: String? = null

        fun build(): ZincArray {
            val elementType = requireNotNull(elementType) { "Required value `elementType` is null." }
            val size = requireNotNull(size) { "Required value `size` is null." }
            return ImmutableZincArray(
                "[${elementType.getId()}; $size]",
                elementType,
                size
            )
        }
    }

    companion object {
        private data class ImmutableZincArray(
            private val id: String,
            private val elementType: ZincType,
            private val size: String
        ) : ZincArray {
            override fun getId(): String = id
            override fun getElementType(): ZincType = elementType
            override fun getSize(): String = size
        }

        fun zincArray(init: Builder.() -> Unit): ZincArray = Builder().apply(init).build()
    }
}
