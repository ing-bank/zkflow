package com.ing.zkflow.serialization.utils.binary

import com.ing.zkflow.serialization.scheme.BFLScheme
import com.ing.zkflow.serialization.scheme.BFLSchemeBits
import com.ing.zkflow.serialization.scheme.BFLSchemeBytes

/**
 * This module is a collection of the transformation utilities from basic types to their binary representation.
 * Common denominator for bit and byte representation is an IntArray (or UByte).
 */

sealed class Representation {
    abstract val serializationScheme: BFLScheme

    object BITS : Representation() {
        override val serializationScheme = BFLSchemeBits
    }

    object BYTES : Representation() {
        override val serializationScheme = BFLSchemeBytes
    }
}
