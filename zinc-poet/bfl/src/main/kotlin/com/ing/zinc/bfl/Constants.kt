package com.ing.zinc.bfl

/** Module name for module that holds constants. */
const val CONSTS = "consts"
/** Variable name for offset variable. */
const val OFFSET = "offset"
/** Variable name for the variable that holds the serialized witness group. */
const val SERIALIZED = "serialized"
/** Number of bits that corda prepends to serialized witness groups. */
const val CORDA_MAGIC_BITS_SIZE = 7 * Byte.SIZE_BITS
/** Constant name of the constant that holds the number of bits that corda prepends to serialized witness groups. */
const val CORDA_MAGIC_BITS_SIZE_CONSTANT_NAME = "CORDA_MAGIC_BITS_SIZE"
/** Import for the CORDA_MAGIC_BITS_SIZE constant. */
const val CORDA_MAGIC_BITS_SIZE_CONSTANT = "$CONSTS::$CORDA_MAGIC_BITS_SIZE_CONSTANT_NAME"
