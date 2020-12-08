package com.ing.zknotary.descriptors

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.UseDefault
import com.ing.zknotary.descriptors.types.AnnotatedSizedClassDescriptor
import com.ing.zknotary.descriptors.types.DefaultableClassDescriptor
import com.ing.zknotary.util.findAnnotation

/**
 * Decomposition of any type happens within a context of
 * classes annotated with `Sized`.
 * For such classes it is assumed that respective empty constructors will be generated.
 *
 * If class corresponding to a given type argument is not annotated with `Sized`,
 * the type argument itself can be annotated with `UseDefault`. For such classes
 * presence of an empty constructor must be validated.
 *
 * If in any case the empty constructor can be invoked, a respective
 * version of `TypeDescriptor` will be returned.
 */

class DescriptionContext(val annotatedClasses: List<KSClassDeclaration>) {
    fun describe(type: KSType): TypeDescriptor? {
        val clazz = type.declaration as? KSClassDeclaration
            ?: error("$type is not a class and cannot be instantiated")

        val typename = "${type.declaration}"

        // Check if this type is annotated with `Sized`:
        // type will (or already) have a generated fixed length version.
        if (annotatedClasses.any { it.simpleName.asString() == typename }) {
            return AnnotatedSizedClassDescriptor(clazz)
        }

        // Check if this type is annotated with `UseDefault`
        type.findAnnotation<UseDefault>() ?: return null

        if (clazz.getConstructors().any {
                it.isPublic() && it.parameters.isEmpty()
            }) {
            return DefaultableClassDescriptor(clazz)
        }

        return null
    }
}