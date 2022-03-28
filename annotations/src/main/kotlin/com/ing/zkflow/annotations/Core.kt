package com.ing.zkflow.annotations

import com.ing.zkflow.ConversionProvider
import kotlin.reflect.KClass

/**
 * Bound collection size, e.g., List, Map, String
 */
@Target(AnnotationTarget.TYPE)
annotation class Size(val size: Int)

// In a UTF-8 string, each character occupies 2 bytes.
@Target(AnnotationTarget.TYPE)
annotation class UTF8Char

/**
 * In an ASCII string, each character occupies 1 byte.
 */
@Target(AnnotationTarget.TYPE)
annotation class ASCIIChar

/**
 * Designate _entry_ classes for ZKP serializable classes.
 */
annotation class ZKP

annotation class ZKPSurrogate(val provider: KClass<out ConversionProvider<*, *>>)

/**
 * Select representation of a floating-point type.
 */
@Target(AnnotationTarget.TYPE)
annotation class BigDecimalSize(val integerPart: Int, val fractionPart: Int)
