package com.ing.zinc.bfl

data class BflMapEntry(
    val keyType: BflType,
    val valueType: BflType,
) : BflStruct(
    "${keyType.typeName()}To${valueType.typeName()}MapEntry",
    listOf(
        Field(KEY_FIELD, keyType),
        Field(VALUE_FIELD, valueType),
    )
) {
    companion object {
        const val KEY_FIELD = "key"
        const val VALUE_FIELD = "value"
    }
}
