package com.ing.zknotary.descriptors

import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.FixToLength
import com.ing.zknotary.descriptors.types.ListDescriptor
import com.ing.zknotary.descriptors.types.PairDescriptor
import com.ing.zknotary.descriptors.types.PrimitiveTypeDescriptor
import com.ing.zknotary.descriptors.types.StringDescriptor
import com.ing.zknotary.descriptors.types.TripleDescriptor
import com.ing.zknotary.util.asClassName
import com.ing.zknotary.util.findAnnotation
import com.ing.zknotary.util.findArgument
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
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
 *   TypeDescriptor.IntDescriptor                      TypeDescriptor.IntTypeDescriptor
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

        private val collectionTypes = listOf(
            ByteArray::class.simpleName,
            ShortArray::class.simpleName,
            IntArray::class.simpleName,
            LongArray::class.simpleName,
            BooleanArray::class.simpleName,
            CharArray::class.simpleName,
            // Exact type of the array does not matter, type is required to get `simpleName` in an "agreed" way.
            Array<Unit>::class.simpleName,
            List::class.simpleName
        ).map { it!! }

        private val supported = primitiveTypes + compoundTypes + collectionTypes

        fun of(type: KSType, context: DescriptionContext): TypeDescriptor {
            ofPrimitiveType(type) ?.let { return it }
            ofCompoundType(type, context) ?.let { return it }
            ofCollectionTypes(type, context) ?.let { return it }

            throw SupportException.UnsupportedNativeType(type, supported)
        }

        private fun ofPrimitiveType(type: KSType): TypeDescriptor? =
            when ("${type.declaration}") {
                Byte::class.simpleName -> PrimitiveTypeDescriptor<Byte>(0, type.declaration.asClassName)
                Short::class.simpleName -> PrimitiveTypeDescriptor<Short>(0, type.declaration.asClassName)
                Int::class.simpleName -> PrimitiveTypeDescriptor<Int>(0, type.declaration.asClassName)
                Long::class.simpleName -> PrimitiveTypeDescriptor<Long>(0, type.declaration.asClassName)
                Boolean::class.simpleName -> PrimitiveTypeDescriptor(false, type.declaration.asClassName)
                Char::class.simpleName -> PrimitiveTypeDescriptor("'0'", type.declaration.asClassName)
                else -> null
            }

        private fun ofCompoundType(type: KSType, context: DescriptionContext): TypeDescriptor? =
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

                else -> null
            }

        private fun ofCollectionTypes(type: KSType, context: DescriptionContext): TypeDescriptor? {
            val declaration = "${type.declaration}"
            if (!collectionTypes.contains(declaration)) {
                return null
            }

            val size = type.expectArgFixToLength()

            return when (declaration) {
                ByteArray::class.simpleName ->
                    ListDescriptor(size, listOf(PrimitiveTypeDescriptor<Byte>(0, Byte::class.asClassName())))
                ShortArray::class.simpleName ->
                    ListDescriptor(size, listOf(PrimitiveTypeDescriptor<Short>(0, Short::class.asClassName())))
                IntArray::class.simpleName ->
                    ListDescriptor(size, listOf(PrimitiveTypeDescriptor<Int>(0, Int::class.asClassName())))
                LongArray::class.simpleName ->
                    ListDescriptor(size, listOf(PrimitiveTypeDescriptor<Long>(0, Long::class.asClassName())))
                BooleanArray::class.simpleName ->
                    ListDescriptor(size, listOf(PrimitiveTypeDescriptor<Boolean>(false, Boolean::class.asClassName())))
                CharArray::class.simpleName ->
                    ListDescriptor(size, listOf(PrimitiveTypeDescriptor("'0'", Char::class.asClassName())))
                Array<Unit>::class.simpleName -> {
                    // Arrays must have an inner type.
                    val arrayType = type.arguments.single().type?.resolve()
                        ?: throw CodeException.InvalidDeclaration(type)

                    ListDescriptor(size, listOf(arrayType.describe(context)))
                }
                List::class.simpleName -> {
                    // List must have an inner type.
                    val listType = type.arguments.single().type?.resolve()
                        ?: throw CodeException.InvalidDeclaration(type)

                    ListDescriptor(size, listOf(listType.describe(context)))
                }

                else -> null
            }
        }

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
                        throw CodeException.InvalidAnnotationArgument(FixToLength::class.simpleName!!, "size")
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
