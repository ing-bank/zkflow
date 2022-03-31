package com.ing.zkflow.serialization.serializer.string

import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.util.require

/**
 * Serializer for [String], using UTF-32 encoding. The UTF-32 encoded byte array is restrained by [maxBytes].
 *
 * UTF-32 is a fixed-length character encoding, where each character will always be encoded in 32 bits.
 */
open class FixedLengthUtf32StringSerializer(maxBytes: Int) :
    AbstractFixedLengthStringSerializer(
        maxBytes.require({ it % Int.SIZE_BYTES == 0 }) {
            "Maximum number of bytes in a ${Charsets.UTF_32.name()} string must be a multiple of ${Int.SIZE_BYTES}."
        },
        FixedLengthType.UTF32_STRING,
        Charsets.UTF_32,
    )
