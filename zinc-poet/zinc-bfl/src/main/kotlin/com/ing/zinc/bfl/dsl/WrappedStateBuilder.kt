package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflWrappedState
import com.ing.zinc.bfl.CORDA_MAGIC_BYTES_SIZE
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.naming.camelToSnakeCase

@BflDslMarker
class WrappedStateBuilder {
    var name: String? = null
    private val fields: MutableList<BflStructField> = mutableListOf()

    private fun field(init: FieldBuilder.() -> Unit) {
        val field = FieldBuilder().apply(init).build()
        fields.add(field)
    }

    /**
     * Add field with corda magic prefix.
     */
    fun cordaMagic() {
        field {
            name = "corda_magic"
            type = array {
                capacity = CORDA_MAGIC_BYTES_SIZE
                elementType = BflPrimitive.I8
            }
        }
    }

    /**
     * Add metadata, when a struct, all fields are (recursively) inlined.
     */
    fun metadata(metadataType: BflType) {
        return metadata(metadataType, metadataType.typeName().camelToSnakeCase())
    }

    /**
     * Add metadata, when a struct, all fields are (recursively) inlined.
     */
    private fun metadata(metadataType: BflType, fieldName: String) {
        when (metadataType) {
            is BflStruct -> {
                metadataType.fields.forEach { field ->
                    metadata(field.type, fieldName.appendFieldName(field.name))
                }
            }
            else -> field {
                name = fieldName
                type = metadataType
            }
        }
    }

    /**
     * Add state field with [stateType].
     * In practice this should always be the last field in the [BflWrappedState].
     */
    fun state(stateType: BflType) {
        field {
            name = getFieldNameFor(stateType)
            type = stateType
        }
    }

    fun build() = BflWrappedState(
        requireNotNull(name) { "Struct property id is missing" },
        fields,
    )

    companion object {
        fun wrappedState(init: WrappedStateBuilder.() -> Unit): BflWrappedState = WrappedStateBuilder().apply(init).build()

        private fun getFieldNameFor(stateType: BflType): String {
            return stateType.typeName().camelToSnakeCase().let {
                if (BflPrimitive.isPrimitiveIdentifier(it)) {
                    "${it}_field"
                } else {
                    it
                }
            }
        }

        private fun String.appendFieldName(fieldName: String): String {
            return if (endsWith(fieldName)) {
                this
            } else {
                this + "_" + fieldName
            }
        }
    }
}
