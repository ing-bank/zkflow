package com.ing.zknotary.descriptors

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
 *                          TypeDescriptor.List_
 *                          (inner descriptors)
 *                                  |
 *                          TypeDescriptor.Pair_
 *                          (inner descriptors)
 *          |----------------------|-------------------------------|
 *          |                                                      |
 *   TypeDescriptor.Int_                                    TypeDescriptor.Int_
 *
 * Each version of TypeDescriptor implements a bespoke functionality
 * to construct default values and to create values of the right type from a variable.
 *
 * In the previous example, examples of such functionality are:
 * - TypeDescriptor.List_ implements replacement of `List` to `WrappedList`
 * and maps contained elements into sized versions;
 * - TypeDescriptor.Pair_ constructs a pair making use of `first` and `second` fields.
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
    )
{
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
