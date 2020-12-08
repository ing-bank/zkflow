package com.ing.zknotary.descriptors

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.FixLength
import com.ing.zknotary.descriptors.types.AnnotatedSizedClassDescriptor
import com.ing.zknotary.descriptors.types.DefaultableClassDescriptor
import com.ing.zknotary.descriptors.types.IntDescriptor
import com.ing.zknotary.descriptors.types.ListDescriptor
import com.ing.zknotary.descriptors.types.PairDescriptor
import com.ing.zknotary.descriptors.types.TripleDescriptor
import com.ing.zknotary.generator.log
import com.ing.zknotary.util.expectArgument
import com.ing.zknotary.util.findAnnotation
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
    // data class Result private constructor (val descriptor: TypeDescriptor?, val error: String?) {
    //     companion object {
    //         fun ok(descriptor: TypeDescriptor) = Result(descriptor, null)
    //         fun err(reason: String) = Result(null, reason)
    //     }
    // }

    companion object {
        val supported = listOf(
            Int::class.simpleName,
            Pair::class.simpleName,
            Triple::class.simpleName,
            List::class.simpleName
        ).map { it!!}

        fun of(type: KSType, context: DescriptionContext): TypeDescriptor? {
            return when ("${type.declaration}") {
                // Primitive types
                Int::class.simpleName -> IntDescriptor(0, type.declaration)

                //
                // Compound types
                Pair::class.simpleName -> PairDescriptor(
                    type.arguments.subList(0, 2).map {
                        val innerType = it.type?.resolve()
                        require(innerType != null) { "Pair must have type arguments" }
                        innerType.describe(context)
                    }
                )

                Triple::class.simpleName -> TripleDescriptor(
                    type.arguments.subList(0, 3).map {
                        val innerType = it.type?.resolve()
                        require(innerType != null) { "Pair must have type arguments" }
                        innerType.describe(context)
                    }
                )

                //
                // Collections
                List::class.simpleName -> {
                    // List must be annotated with Sized.
                    val fixedLength = type.findAnnotation<FixLength>()
                        ?: error("Collection must be annotated with `FixLength`")

                    // TODO Too many hardcoded things: property names and their types.
                    val size = fixedLength.expectArgument<Int>("size")

                    // List must have an inner type.
                    val listType = type.arguments.single().type?.resolve()
                        ?: error("List must have a type")


                    ListDescriptor(size, listOf(listType.describe(context)))
                }

                //
                else -> null
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
