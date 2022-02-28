package com.ing.zinc.poet

import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zkflow.util.requireNotEmpty
import com.ing.zkflow.util.requireNotNull

interface ZincStruct : ZincType, ZincFileItem {
    fun getName(): String
    fun getFields(): List<ZincField>

    @ZincDslMarker
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
            name.requireNotNull { "Required value `name` is null." },
            // Zinc allows empty structs, but raises unexpected error messages that cannot be traced back to empty structs.
            // Therefor we forbid constructing empty structs here.
            fields.toList().requireNotEmpty { "Struct `$name` has no fields." },
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
