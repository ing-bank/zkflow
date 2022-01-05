package com.ing.zinc.poet

interface ZincFile {
    fun getFileItems(): List<ZincFileItem>
    // TODO generating a plain string is not the fastest method, maybe use [PrintWriter]?
    fun generate(): String

    @ZincDslMarker
    @Suppress("TooManyFunctions")
    class Builder {
        private val fileItems: MutableList<ZincFileItem> = mutableListOf()

        fun constant(init: ZincConstant.Builder.() -> Unit): Builder {
            fileItems.add(ZincConstant.zincConstant(init))
            return this
        }

        fun function(init: ZincFunction.Builder.() -> Unit): Builder {
            fileItems.add(ZincFunction.zincFunction(init))
            return this
        }

        fun mod(init: ZincMod.Builder.() -> Unit): Builder {
            fileItems.add(ZincMod.zincMod(init))
            return this
        }

        fun use(init: ZincUse.Builder.() -> Unit): Builder {
            fileItems.add(ZincUse.zincUse(init))
            return this
        }

        fun struct(init: ZincStruct.Builder.() -> Unit): Builder {
            fileItems.add(ZincStruct.zincStruct(init))
            return this
        }

        fun impl(init: ZincImpl.Builder.() -> Unit): Builder {
            fileItems.add(ZincImpl.zincImpl(init))
            return this
        }

        fun enum(init: ZincEnum.Builder.() -> Unit): Builder {
            fileItems.add(ZincEnum.zincEnum(init))
            return this
        }

        fun type(init: ZincTypeDef.Builder.() -> Unit): Builder {
            fileItems.add(ZincTypeDef.zincTypeDef(init))
            return this
        }

        fun newLine(): Builder {
            fileItems.add(ZincNewline.zincNewline())
            return this
        }

        fun comment(comment: String): Builder {
            fileItems.add(ZincComment.zincComment(comment))
            return this
        }

        fun add(fileItem: ZincFileItem): Builder {
            fileItems.add(fileItem)
            return this
        }

        fun addAll(fileItems: Collection<ZincFileItem>): Builder {
            this.fileItems.addAll(fileItems)
            return this
        }

        fun build(): ZincFile {
            return ImmutableZincFile(
                fileItems.toList()
            )
        }
    }

    companion object {
        private data class ImmutableZincFile(
            private val fileItems: List<ZincFileItem>
        ) : ZincFile {
            override fun getFileItems(): List<ZincFileItem> = fileItems
            override fun generate(): String {
                return getFileItems().joinToString("\n") { it.generate() }
            }
        }

        fun zincFile(init: Builder.() -> Unit): ZincFile = Builder().apply(init).build()
    }
}
