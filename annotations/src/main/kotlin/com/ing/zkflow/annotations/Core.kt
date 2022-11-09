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
 * When the String contains characters not supported by the ISO-8859-1 charset, an IllegalStateException will be thrown.
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
 * Classes annotated with @ZKP can be used in ZKFlow transactions.
 * All ContractStates and CommandDatas that you want to use in a transaction must have this annotation.
 * The same is true for any type that you use for the properties of these states and commands.
 *
 * If you want to use a type that you can't annotate, either extend it or create a surrogate for it.
 * It is recommended to not use too many third party types, since this increases versioning complexity a lot.
 * It is even recommended to keep your states and commands as flat as possible in general, to keep versioning
 * as simple as possible.
 *
 * Note that @ZKP-annotated classes are considered stable and can never change after they have been deployed.
 * If you have to make changes, you will need to introduce a new type, as ZKFlow will
 * prohibit you from making changes after first deployment of the class.
 */
@Target(AnnotationTarget.CLASS)
annotation class ZKP

/**
 * Use this annotation on a class that implements Surrogate<*>.
 *
 * Note that @ZKPSurrogate-annotated classes are considered stable and can never change after they have been deployed.
 * If you have to make changes, you will need to introduce a new type, as ZKFlow will
 * prohibit you from making changes after first deployment of the class.
 */
@Target(AnnotationTarget.CLASS)
annotation class ZKPSurrogate(val provider: KClass<out ConversionProvider<*, *>>)

/**
 * Use this annotation to annotate abstract classes or interfaces that you consider stable and
 * whose properties you want to use directly in implementors.
 *
 * Do not use this annotation on concrete classes or objects. Use ZKPSurrogate or ZKP annotations for those.
 *
 * We allow direct usage of properties of certain parent classes only if they are:
 * - core Corda classes, such as ContractState, OwnableState, etc.
 * - annotated with @ZKPStable.
 * These classes are considered stable, i.e. never changing.
 * For Corda classes this is true unless the Corda platform version changes.
 * ZKFlow is already tied to a specific Corda platform version.
 *
 * Note that @ZKPStable-annotated classes are considered stable and can never change after they have been deployed.
 * If you have to make changes, you will need to introduce a new type, as ZKFlow will
 * prohibit you from making changes after first deployment of the class.
 */
@Target(AnnotationTarget.CLASS)
annotation class ZKPStable

/**
 * Select representation of a floating-point type.
 */
@Target(AnnotationTarget.TYPE)
annotation class BigDecimalSize(val integerPart: Int, val fractionPart: Int)
