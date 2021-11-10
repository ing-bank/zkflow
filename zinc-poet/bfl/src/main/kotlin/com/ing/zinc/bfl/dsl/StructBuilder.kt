package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.BflStructField

class StructBuilder {
    var name: String? = null
    private val fields: MutableList<BflStructField> = mutableListOf()

    fun field(init: FieldBuilder.() -> Unit) {
        val field = FieldBuilder().apply(init).build()
        fields.add(field)
    }

    fun addFields(fields: Collection<BflStructField>) {
        this.fields.addAll(fields)
    }

    fun build() = BflStruct(
        requireNotNull(name) { "Struct property id is missing" },
        fields,
    )

    companion object {
        fun struct(init: StructBuilder.() -> Unit): BflStruct = StructBuilder().apply(init).build()
    }
}
