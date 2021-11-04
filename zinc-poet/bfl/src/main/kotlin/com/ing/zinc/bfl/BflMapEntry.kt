package com.ing.zinc.bfl

data class BflMapEntry(
    val keyType: BflType,
    val valueType: BflType,
) : BflStruct(
    "${keyType.typeName()}To${valueType.typeName()}MapEntry",
    listOf(
        Field(keyFieldName, keyType),
        Field(valueFieldName, valueType),
    )
) {
    companion object {
        const val keyFieldName = "key"
        const val valueFieldName = "value"
    }
}
