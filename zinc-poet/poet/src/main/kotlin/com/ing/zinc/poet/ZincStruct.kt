package com.ing.zinc.poet

import com.ing.zinc.poet.Indentation.Companion.spaces

interface ZincStruct : ZincType, ZincFileItem {
    fun getName(): String
    fun getFields(): List<ZincField>

    class Builder {
        var name: String? = null
        private val fields: MutableList<ZincField> = mutableListOf()

        fun addField(field: ZincField): Builder {
            this.fields.add(field)
            return this
        }

        fun addFields(fields: Collection<ZincField>): Builder {
            this.fields.addAll(fields)
            return this
        }

        fun field(init: ZincField.Builder.() -> Unit): Builder {
            this.fields.add(ZincField.zincField(init))
            return this
        }

        fun build(): ZincStruct = ImmutableZincStruct(
            requireNotNull(name) { "Required value `name` is null." },
            fields.toList(),
        )
    }

    companion object {
        private data class ImmutableZincStruct(
            private val name: String,
            private val fields: List<ZincField>,
        ) : ZincStruct {
            override fun getId(): String = name
            override fun generate(): String {
                val fieldString = getFields().joinToString("\n") {
                    "${it.getName()}: ${it.getType().getId()},"
                }
                return """
                    struct ${getName()} {
                        ${fieldString.indent(24.spaces)}
                    }
                """.trimIndent()
            }

            override fun getName(): String = name
            override fun getFields(): List<ZincField> = fields
        }

        fun zincStruct(init: Builder.() -> Unit): ZincStruct = Builder().apply(init).build()
    }
}
