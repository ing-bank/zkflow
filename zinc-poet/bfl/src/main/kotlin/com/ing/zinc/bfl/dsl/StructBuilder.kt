package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod

class StructBuilder {
    var id: String? = null
    private val fields: MutableList<BflStruct.Field> = mutableListOf()
    private val methods: MutableList<ZincFunction> = mutableListOf()

    fun field(init: FieldBuilder.() -> Unit) {
        val field = FieldBuilder().apply(init).build()
        fields.add(field)
    }

    fun addFields(fields: Collection<BflStruct.Field>) {
        this.fields.addAll(fields)
    }

    fun method(init: ZincMethod.Builder.() -> Unit) {
        this.methods.add(ZincMethod.zincMethod(init))
    }

    fun function(init: ZincFunction.Builder.() -> Unit) {
        this.methods.add(ZincFunction.zincFunction(init))
    }

    fun build() = BflStruct(
        requireNotNull(id) { "Struct property id is missing" },
        fields,
        methods
    )

    companion object {
        fun struct(init: StructBuilder.() -> Unit): BflStruct = StructBuilder().apply(init).build()
    }
}
