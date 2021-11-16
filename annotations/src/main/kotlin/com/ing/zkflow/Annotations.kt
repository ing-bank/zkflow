package com.ing.zkflow

import kotlin.reflect.KClass

// User domain                         Serde domain                              Zinc domain
// Some nice annotations  -- Arrow ->  Surrogate representations  -- kotlinx --> zinc serializations
//                                     Serializers                |
//                                     @Serializable(with)        |------------> zinc code
//

// Core annotations ==>

/**
 * Bound collection size, e.g., List, Map
 */
@Target(AnnotationTarget.TYPE)
annotation class Size(val size: Int)

/**
 * Select UTF-8 representation and max length of a string a string.
 */
@Target(AnnotationTarget.TYPE)
annotation class UTF8(val length: Int)

// In a UTF-8 string, each character occupies 2 bytes.
@Target(AnnotationTarget.TYPE)
annotation class UTF8Char

/**
 * Select ASCII representation and max length of a string a string.
 */
@Target(AnnotationTarget.TYPE)
annotation class ASCII(val length: Int)

// In an ASCII string, each character occupies 1 byte.
@Target(AnnotationTarget.TYPE)
annotation class ASCIIChar

/**
 * Default value for a nullable value or a (shorter than expected) collection.
 */
interface DefaultProvider<T : Any> {
    val default: T
}

@Target(AnnotationTarget.TYPE)
annotation class Default<T : Any>(val provider: KClass<out DefaultProvider<T>>)

/**
 * 3rd party classes will require conversion to/from surrogates.
 */
interface ConversionProvider<T : Any, S : Surrogate<T>> {
    fun from(original: T): S
}

@Target(AnnotationTarget.TYPE)
// TODO get rid of S here and com.ing.zkflow.Resolver
annotation class Converter<T : Any, S : Surrogate<T>>(val provider: KClass<out ConversionProvider<T, out Surrogate<T>>>)

/**
 * Conveniently define default provider and resolver.
 */
@Target(AnnotationTarget.TYPE)
annotation class Resolver<T : Any, S : Surrogate<T>>(
    val defaultProvider: KClass<out DefaultProvider<T>>,
    val converterProvider: KClass<out ConversionProvider<T, out Surrogate<T>>>
)

/**
 * Designate _entry_ classes for ZKP serializable classes.
 */
annotation class ZKP

/**
 * Select representation of a floating-point type.
 */
@Target(AnnotationTarget.TYPE)
annotation class BigDecimalSize(val integerPart: Int, val fractionPart: Int)

/**
 * Surrogate is a specific representation of a class, such representation should allow for simpler serialization.
 * This definition also ensures all surrogate classes can convert itself back to originals.
 */
interface Surrogate<T> {
    fun toOriginal(): T
}
