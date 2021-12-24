package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod

class StructBuilder {
    var name: String? = null
    private val fields: MutableList<BflStructField> = mutableListOf()
    private val functions: MutableList<ZincFunction> = mutableListOf()
    var isDeserializable: Boolean = true

    fun field(init: FieldBuilder.() -> Unit) {
        val field = FieldBuilder().apply(init).build()
        fields.add(field)
    }

    fun addFields(fields: Collection<BflStructField>) {
        this.fields.addAll(fields)
    }

    fun method(init: ZincMethod.Builder.() -> Unit) {
        val method = ZincMethod.Builder().apply(init).build()
        functions.add(method)
    }

    fun function(init: ZincFunction.Builder.() -> Unit) {
        val method = ZincFunction.Builder().apply(init).build()
        functions.add(method)
    }

    fun build() = BflStruct(
        requireNotNull(name) { "Struct property id is missing" },
        fields,
        functions,
        isDeserializable,
    )

    companion object {
        fun struct(init: StructBuilder.() -> Unit): BflStruct = StructBuilder().apply(init).build()
    }
}
