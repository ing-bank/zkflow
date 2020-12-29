package com.ing.zknotary.descriptors

import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.FixToLength
import com.ing.zknotary.descriptors.types.ListDescriptor
import com.ing.zknotary.descriptors.types.PairDescriptor
import com.ing.zknotary.descriptors.types.PrimitiveTypeDescriptor
import com.ing.zknotary.descriptors.types.StringDescriptor
import com.ing.zknotary.descriptors.types.TripleDescriptor
import com.ing.zknotary.util.findAnnotation
import com.ing.zknotary.util.findArgument
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
 *   TypeDescriptor.PrimitiveTypeDescriptor                                    TypeDescriptor.PrimitiveTypeDescriptor
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
    constructor(clazz: KClass<*>, innerDescriptors: List<TypeDescriptor> = listOf()) : this(
        ClassName(
            clazz.java.`package`.name,
            listOf(clazz.simpleName!!)
        ),
        innerDescriptors
    )

    companion object {
        private val primitiveTypes = listOf(
            Byte::class.simpleName,
            Short::class.simpleName,
            Int::class.simpleName,
            Long::class.simpleName,
            Boolean::class.simpleName,
            Char::class.simpleName
        ).map { it!! }

        private val compoundTypes = listOf(
            String::class.simpleName,
            Pair::class.simpleName,
            Triple::class.simpleName
        ).map { it!! }

        private val collectionTypes = listOf(List::class.simpleName).map { it!! }

        private val supported = primitiveTypes + compoundTypes + collectionTypes

        @Suppress("ComplexMethod")
        fun of(type: KSType, context: DescriptionContext): TypeDescriptor {
            val declaration = "${type.declaration}"
            return when {
                declaration belongsTo primitiveTypes -> ofPrimitiveType(type)
                declaration belongsTo compoundTypes -> ofCompoundType(type, context)
                declaration belongsTo collectionTypes -> ofCollectionTypes(type, context)
                else -> throw SupportException.UnsupportedNativeType(type, supported)
            }
        }

        private fun ofPrimitiveType(type: KSType): TypeDescriptor =
            when ("${type.declaration}") {
                Byte::class.simpleName -> PrimitiveTypeDescriptor<Byte>(0, type.declaration)
                Short::class.simpleName -> PrimitiveTypeDescriptor<Short>(0, type.declaration)
                Int::class.simpleName -> PrimitiveTypeDescriptor<Int>(0, type.declaration)
                Long::class.simpleName -> PrimitiveTypeDescriptor<Long>(0, type.declaration)
                Boolean::class.simpleName -> PrimitiveTypeDescriptor(false, type.declaration)
                Char::class.simpleName -> PrimitiveTypeDescriptor("'0'", type.declaration)
                else -> error("Unexpected error. Type ${type.declaration} must be in the list of primitive types.")
            }

        private fun ofCompoundType(type: KSType, context: DescriptionContext): TypeDescriptor =
            when ("${type.declaration}") {
                String::class.simpleName -> StringDescriptor(length = type.expectArgFixToLength(), filler = '0')
                Pair::class.simpleName -> PairDescriptor(
                    type.arguments.subList(0, 2).map {
                        val innerType = it.type?.resolve()
                            ?: throw CodeException.InvalidDeclaration(type)
                        innerType.describe(context)
                    }
                )

                Triple::class.simpleName -> TripleDescriptor(
                    type.arguments.subList(0, 3).map {
                        val innerType = it.type?.resolve()
                            ?: throw CodeException.InvalidDeclaration(type)
                        innerType.describe(context)
                    }
                )

                else -> error("Unexpected error. Type ${type.declaration} must be in the list of compound types.")
            }

        private fun ofCollectionTypes(type: KSType, context: DescriptionContext): TypeDescriptor =
            when ("${type.declaration}") {
                List::class.simpleName -> {
                    val size = type.expectArgFixToLength()

                    // List must have an inner type.
                    val listType = type.arguments.single().type?.resolve()
                        ?: throw CodeException.InvalidDeclaration(type)

                    ListDescriptor(size, listOf(listType.describe(context)))
                }

                else -> error("Unexpected error. Type ${type.declaration} must be in the list of collection types.")
            }

        private infix fun <T> T.belongsTo(list: List<T>) = list.contains(this)

        private fun KSType.expectArgFixToLength(): Int =
            Result.success(this)
                .mapCatching { type ->
                    type.findAnnotation<FixToLength>() ?: throw CodeException.MissingAnnotation(
                        this,
                        FixToLength::class
                    )
                }
                .mapCatching { ann ->
                    ann.findArgument<Int>("size") ?: throw CodeException.InvalidAnnotation(FixToLength::class, "size")
                }
                .mapCatching { size ->
                    if (size <= 0) {
                        throw CodeException.InvalidAnnotationArgument(FixToLength::class, "size")
                    }
                    size
                }
                .getOrThrow()
    }

    abstract val default: CodeBlock
    abstract fun toCodeBlock(propertyName: String): CodeBlock

    open val isTransient: Boolean = true

    open val type: TypeName
        get() = definition.parameterizedBy(innerDescriptors.map { it.type })

    open val ownImports: List<ClassName> = listOf()
    fun imports(): List<ClassName> = innerDescriptors.fold(ownImports) { total, item -> total + item.imports() }
}
