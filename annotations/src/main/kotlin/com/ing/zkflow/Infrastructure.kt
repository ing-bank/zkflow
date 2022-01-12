package com.ing.zkflow

import kotlin.reflect.KClass

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
annotation class Converter<T : Any, S : Surrogate<T>>(val provider: KClass<out ConversionProvider<T, out S>>)

/**
 * Conveniently define default provider and resolver.
 */
@Target(AnnotationTarget.TYPE)
annotation class Resolver<T : Any, S : Surrogate<T>>(
    val defaultProvider: KClass<out DefaultProvider<T>>,
    val converterProvider: KClass<out ConversionProvider<T, out S>>
)

/**
 * Surrogate is a specific representation of a class, such representation should allow for simpler serialization.
 * This definition also ensures all surrogate classes can convert itself back to originals.
 */
interface Surrogate<T> {
    fun toOriginal(): T
}
