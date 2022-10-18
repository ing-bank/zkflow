package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflWrappedTransactionComponent
import com.ing.zinc.bfl.CORDA_MAGIC_BYTES_SIZE
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.naming.camelToZincSnakeCase
import com.ing.zinc.poet.ZincPrimitive

@BflDslMarker
class WrappedTransactionComponentBuilder {
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
     * Add metadata.
     * When [type] is a struct, all fields are (recursively) inlined.
     * This is done to prevent generation of unused structs in the circuit folder.
     */
    fun metadata(type: BflType) {
        return metadata(type, type.typeName().camelToZincSnakeCase())
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
     * Add transaction component field with [type].
     * In practice this should always be the last field in the [BflWrappedTransactionComponent].
     */
    fun transactionComponent(type: BflType) {
        field {
            name = getFieldNameFor(type)
            this.type = type
        }
    }

    fun build() = BflWrappedTransactionComponent(
        requireNotNull(name) { "Struct property id is missing" },
        fields,
    )

    companion object {
        fun wrappedTransactionComponent(init: WrappedTransactionComponentBuilder.() -> Unit): BflWrappedTransactionComponent = WrappedTransactionComponentBuilder().apply(init).build()

        private fun getFieldNameFor(type: BflType): String {
            return type.typeName().camelToZincSnakeCase().let {
                if (ZincPrimitive.isPrimitiveIdentifier(it)) {
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
