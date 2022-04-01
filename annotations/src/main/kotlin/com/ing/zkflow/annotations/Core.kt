package com.ing.zkflow.annotations

import com.ing.zkflow.ConversionProvider
import kotlin.reflect.KClass

/**
 * Bound collection size, e.g., List, Map.
 */
@Target(AnnotationTarget.TYPE)
annotation class Size(val size: Int)

/**
 * Bound size of ASCII encoded String.
 *
 * This is a fixed-length encoding scheme where every character occupies 1 byte.
 * The actual implementation uses ISO-8859-1 or latin-1 encoding, which is sometimes referred to as extended ASCII.
 * Any character not supported by ISO-8859-1 will be serialized as '?' (ascii code 63).
 *
 * @param byteSize The max number of bytes in the ASCII encoded String.
 */
@Target(AnnotationTarget.TYPE)
annotation class ASCII(val byteSize: Int)

/**
 * Bound size of UTF-8 encoded String.
 *
 * This is a variable-width encoding scheme where the smallest characters occupy a single byte, and the largest
 * characters can contain up to 6 bytes.
 *
 * @param byteSize The max number of bytes in the UTF-8 encoded String.
 */
@Target(AnnotationTarget.TYPE)
annotation class UTF8(val byteSize: Int)

/**
 * Bound size of UTF-16 encoded String.
 *
 * This is a variable-width encoding scheme where the smallest characters occupy 2 bytes, and larger characters can
 * occupy a multiple of 2 bytes.
 *
 * @param byteSize The max number of bytes in the UTF-16 encoded String.
 */
@Target(AnnotationTarget.TYPE)
annotation class UTF16(val byteSize: Int)

/**
 * Bound size of UTF-32 encoded String.
 *
 * This is a fixed-length encoding scheme where every character occupies 4 bytes.
 *
 * @param byteSize The max number of bytes in the UTF-32 encoded String.
 */
@Target(AnnotationTarget.TYPE)
annotation class UTF32(val byteSize: Int)

/**
 * A Unicode character (codepoint) occupies 2 bytes.
 */
@Target(AnnotationTarget.TYPE)
annotation class UnicodeChar

/**
 * An ASCII character occupies 1 byte.
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
