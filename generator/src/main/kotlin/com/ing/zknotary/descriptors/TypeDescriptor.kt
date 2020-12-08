package com.ing.zknotary.descriptors

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.descriptors.types.AnnotatedSizedClassDescriptor
import com.ing.zknotary.descriptors.types.DefaultableClassDescriptor
import com.ing.zknotary.descriptors.types.IntDescriptor
import com.ing.zknotary.descriptors.types.ListDescriptor
import com.ing.zknotary.descriptors.types.PairDescriptor
import com.ing.zknotary.descriptors.types.TripleDescriptor
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

    // 'kotlin.Result' cannot be used as a return type.
    data class Result private constructor (val descriptor: TypeDescriptor?, val error: String?) {
        companion object {
            fun ok(descriptor: TypeDescriptor) = Result(descriptor, null)
            fun err(reason: String) = Result(null, reason)
        }
    }

    companion object {
        fun of(type: KSType, userTypeDescriptor: UserTypeDescriptor): Result {
            val supported = listOf(
                Int::class.simpleName,
                Pair::class.simpleName,
                Triple::class.simpleName,
                List::class.simpleName
            )

            return when ("${type.declaration}") {
                // Primitive types
                Int::class.simpleName -> Result.ok(IntDescriptor(0, type.declaration))

                //
                // Compound types
                Pair::class.simpleName -> Result.ok(PairDescriptor(
                    type.arguments.subList(0, 2).map {
                        val innerType = it.type?.resolve()
                        require(innerType != null) { "Pair must have type arguments" }
                        innerType.describe(userTypeDescriptor)
                    }
                ))

                Triple::class.simpleName -> Result.ok(TripleDescriptor(
                    type.arguments.subList(0, 3).map {
                        val innerType = it.type?.resolve()
                        require(innerType != null) { "Pair must have type arguments" }
                        innerType.describe(userTypeDescriptor)
                    }
                ))

                //
                // Collections
                List::class.simpleName -> Result.ok(ListDescriptor.fromKSP(type, userTypeDescriptor))

                //
                //
                else -> Result.err("Built-in supported types ${supported.joinToString(separator = ",\n")}")
            }
        }

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
 * As it is described in docs for `TypeDescriptor`, the type tree
 * can terminate at use classes which are either annotated with `Sized`
 * or have default constructors which must be used if user requested this.
 *
 * `UserTypeDescriptor` enforces conditions when such termination is possible
 * - type under consideration is listed as `Sized`,
 * - default constructor must be used and it is present.
 *
 * If a respective condition holds, a variant of `TypeDescriptor` will be constructed.
 */

sealed class UserTypeDescriptor {
    abstract fun of(type: KSType): TypeDescriptor.Result

    object Default : UserTypeDescriptor() {
        override fun of(type: KSType): TypeDescriptor.Result {
            // Verify that the type is a user class.
            val clazz = type.declaration as? KSClassDeclaration
                ?: return TypeDescriptor.Result.err("$type is not a user class and cannot be instantiated with a default value")

            // Verify this class has a default (empty) constructor.
            return if (clazz.getConstructors().any {
                    it.isPublic() && it.parameters.isEmpty()
                }) {
                TypeDescriptor.Result.ok(DefaultableClassDescriptor(clazz))
            } else {
                TypeDescriptor.Result.err("$type must have a default (empty) constructor")
            }
        }
    }

    data class Sized(val annotatedClasses: List<KSClassDeclaration>) : UserTypeDescriptor() {
        override fun of(type: KSType): TypeDescriptor.Result {
            val clazz = type.declaration as? KSClassDeclaration
                ?: return TypeDescriptor.Result.err("$type is not a user class and cannot be instantiated with a default value")

            val typename = "${type.declaration}"

            // Check type will (or already) have a generated fixed length version.
            return if (annotatedClasses.any { it.simpleName.asString() == typename }) {
                TypeDescriptor.Result.ok(AnnotatedSizedClassDescriptor(clazz))
            } else {
                TypeDescriptor.Result.err("Class $typename is not expected to have fixed length")
            }
        }
    }
}

