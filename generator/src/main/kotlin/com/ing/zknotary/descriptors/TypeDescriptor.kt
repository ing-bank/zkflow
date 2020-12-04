package com.ing.zknotary.descriptors

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.generator.log
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass

/**
 * `TypeDescriptor` enables description of types parametrized by other types.
 * This description is used to assemble types with required replacements
 * and to create corresponding default values.
 * For example, List<Pair<Int, Int>> is described as follows:
 *                          TypeDescriptor.ListDescriptor
 *                          (inner descriptors)
 *                                  |
 *                          TypeDescriptor.PairDescriptor
 *                          (inner descriptors)
 *          |----------------------|-------------------------------|
 *          |                                                      |
 *   TypeDescriptor.IntDescriptor                                    TypeDescriptor.IntDescriptor
 *
 * Each version of TypeDescriptor implements a bespoke functionality
 * to construct default values and to create values of the right type from a variable.
 *
 * In the previous example, examples of such functionality are:
 * - TypeDescriptor.ListDescriptor implements replacement of `List` to `SizedList`
 * and maps contained elements into sized versions;
 * - TypeDescriptor.PairDescriptor constructs a pair making use of `first` and `second` fields.
 *
 * The TypeDescriptor tree terminates at
 *  - built-in types such as Int, Byte, etc.,
 *  - types for which user has indicated to use respective default constructors, in this
 *    case, user is responsible for making such types have constant size,
 *  - types which have been annotated with Sized and thus the respective default
 *    constructors creating their fixed size versions will be generated.
 */

abstract class TypeDescriptor(
    val definition: ClassName,
    val innerDescriptors: List<TypeDescriptor> = listOf()
) {
    constructor(clazz: KClass<*>, innerDescriptors: List<TypeDescriptor>) : this(
        ClassName(
            clazz.java.`package`.name,
            listOf(clazz.simpleName!!)
        ),
        innerDescriptors
    )

    companion object {
        val supported = listOf(
            Int::class.simpleName,
            Pair::class.simpleName,
            Triple::class.simpleName,
            List::class.simpleName
        )

        fun supports(typeName: String): Boolean =
            supported.contains(typeName)
    }

    abstract val default: CodeBlock
    abstract fun toCodeBlock(propertyName: String): CodeBlock

    open val isTransient: Boolean = true

    open val type: TypeName
        get() = definition.parameterizedBy(innerDescriptors.map { it.type })

    fun debug() {
        if (innerDescriptors.isEmpty()) {
            log?.error("(${(0..100).random()}) $definition : $default")
        } else {
            log?.error("(${(0..100).random()}) $definition")
            innerDescriptors.forEach { it.debug() }
        }
    }
}

/**
 * When building a `TypeDescriptor` for some type, any given type
 * will be decomposed into components.
 * At every level it must hold that a variant of fixed size can be
 * constructed for each type component.
 * Such types are
 * - classes supported by `TypeDescriptor`,
 * - classes that are marked with `Sized` annotation,
 *   i.e., fixed length version of those classes will be or have been generated,
 * - classes for which the generator was instructed to use the default constructor.
 *
 * Support class conveniently offers such verification mechanism.
 * It bundles:
 * - types supported by `TypeDescriptor` and those annotated with `Sized` into SizedClasses.
 *   the former ones are sized by construction and the latter ones by expectation.
 * - types for which user "promises" to have implemented a default constructor creating own fixed length version.
 */
sealed class Support {
    abstract fun requireFor(type: KSType)

    object Default : Support() {
        override fun requireFor(type: KSType) {
            // Verify that the type is a user class.
            val clazz = type.declaration as? KSClassDeclaration
                ?: error("$type is not a user class and cannot be instantiated with a default value")

            // Verify this class has a default (empty) constructor.
            require(
                clazz.getConstructors().any {
                    it.isPublic() && it.parameters.isEmpty()
                }
            ) { "$type must have a default (empty) constructor" }
        }
    }

    data class SizedClasses(val annotatedClasses: List<KSClassDeclaration>) : Support() {
        override fun requireFor(type: KSType) {
            val typename = "${type.declaration}"
            val errors = mutableListOf("Type $typename is not supported\n")

            // Check type is one of those listed in TypeDecriptor
            if (TypeDescriptor.supports(typename)) {
                return
            }
            errors += "Supported types:\n${TypeDescriptor.supported.joinToString(separator = ",\n")}"

            // Check type will (or already) have a generated fixed length version.
            if (annotatedClasses.any { it.simpleName.asString() == typename }) {
                return
            }
            errors += "Class $typename is not expected to have fixed length"

            error(errors.joinToString(separator = "\n"))
        }
    }
}
