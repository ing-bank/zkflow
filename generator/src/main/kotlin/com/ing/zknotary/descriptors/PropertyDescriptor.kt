package com.ing.zknotary.descriptors

/**
 * A convenience class holding data necessary to compose
 * a sized version of some `KSPropertyDeclaration`.
 *
 * For example,
 * ```
 *  class Clazz {
 *      val prop: Int
 *  }
 *  val original = `instance of class`
 * ```
 *
 * A corresponding PropertyDescriptor is
 * PropertyDescriptor {
 *  type: "kotlin.Int",
 *  fromInstance: "original.prop",
 *  default: "0"
 * }
 */
data class PropertyDescriptor(
    val name: String,
    val typeDescriptor: TypeDescriptor
)
