package com.ing.zknotary.common.input

fun toAmountJSON(quantity: Long, displayTokenSizeJSON: String, tokenHash: ByteArray) =
    "{\"quantity\": \"$quantity\", \"display_token_size\": $displayTokenSizeJSON, \"token_name_hash\": ${tokenHash.toPrettyString()}}"

fun toBigDecimalJSON(sign: Byte, integer: ByteArray, fraction: ByteArray) =
    "{\"sign\": \"$sign\", \"integer\": [${integer.joinToString { "\"${it}\"" }}], \"fraction\": ${fraction.toPrettyString()}}"

fun toDoubleJSON(sign: Byte, exponent: Short, magnitude: Long) =
    "{\"exponent\": \"${exponent}\",\"magnitude\": \"${magnitude}\",\"sign\": \"${sign}\"}"

fun ByteArray.toPrettyString() = "[${this.joinToString { "\"${it}\"" }}]"
