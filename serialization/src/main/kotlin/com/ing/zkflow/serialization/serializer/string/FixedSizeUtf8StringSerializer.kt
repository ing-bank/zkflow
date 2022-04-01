package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.FixedLengthType

/**
 * Serializer for [String], using UTF-8 encoding. The UTF-8 encoded byte array is restrained by [maxBytes].
 *
 * UTF-8 is a variable-width character encoding, meaning that single characters may be encoded in multiple positions.
 */
open class FixedSizeUtf8StringSerializer(maxBytes: Int) :
    AbstractFixedSizeStringSerializer(maxBytes, FixedLengthType.UTF8_STRING, Charsets.UTF_8)
