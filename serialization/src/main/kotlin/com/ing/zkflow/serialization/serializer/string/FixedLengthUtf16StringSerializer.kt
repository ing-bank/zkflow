package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.util.require

/**
 * Serializer for [String], using UTF-16 encoding. The UTF-16 encoded byte array is restrained by [maxBytes].
 *
 * UTF-16 is a variable-width character encoding, meaning that single characters may be encoded in multiple positions.
 */
open class FixedLengthUtf16StringSerializer(maxBytes: Int) :
    AbstractFixedLengthStringSerializer(
        maxBytes.require({ it % Short.SIZE_BYTES == 0 }) {
            "Maximum number of bytes in a ${Charsets.UTF_16.name()} string must be a multiple of ${Short.SIZE_BYTES}."
        },
        FixedLengthType.UTF16_STRING,
        Charsets.UTF_16,
    )
