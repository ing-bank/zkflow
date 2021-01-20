package com.ing.zknotary.descriptors

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.Call
import com.ing.zknotary.annotations.CallDefaultValueClass
import com.ing.zknotary.annotations.UseDefault
import com.ing.zknotary.descriptors.types.AnnotatedSizedClassDescriptor
import com.ing.zknotary.descriptors.types.CallableClassDescriptor
import com.ing.zknotary.descriptors.types.CallableKClassDescriptor
import com.ing.zknotary.descriptors.types.DefaultableClassDescriptor
import com.ing.zknotary.util.findAnnotation
import com.ing.zknotary.util.findArgument
import com.squareup.kotlinpoet.ClassName

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

class DescriptionContext(private val annotatedClasses: List<KSClassDeclaration>) {
    fun describe(type: KSType): TypeDescriptor {
        // Check if this type is annotated with `Sized`:
        // type will have or already has a generated fixed length version.
        if (annotatedClasses.any { it.simpleName.asString() == "${type.declaration}" }) {
            val clazz = type.declaration as? KSClassDeclaration ?: throw CodeException.NotAClass("$type")
            return AnnotatedSizedClassDescriptor(clazz)
        }

        type.findAnnotation<UseDefault>()?.let { return processUseDefault(type) }
        type.findAnnotation<Call>()?.let {
            val returnType = ClassName(
                type.declaration.packageName.asString(),
                listOf(type.declaration.simpleName.asString())
            )
            return processCall(it, returnType)
        }
        type.findAnnotation<CallDefaultValueClass>()?.let {
            val returnType = ClassName(
                type.declaration.packageName.asString(),
                listOf(type.declaration.simpleName.asString())
            )
            return processCallKClass(it, returnType)
        }

        throw SupportException.UnsupportedUserType(type, annotatedClasses.map { it.simpleName.asString() })
    }

    private fun processUseDefault(type: KSType): TypeDescriptor {
        val clazz = type.declaration as? KSClassDeclaration ?: throw CodeException.NotAClass("$type")
        if (clazz.getConstructors().none { it.isPublic() && it.parameters.isEmpty() }) {
            throw CodeException.DefaultConstructorAbsent(type)
        }
        return DefaultableClassDescriptor(clazz)
    }

    private fun processCall(callAnnotation: KSAnnotation, returnType: ClassName): TypeDescriptor {
        val imports = findImports(callAnnotation)

        val code = callAnnotation.findArgument<String>("code")
            ?: throw CodeException.InvalidAnnotationArgument(callAnnotation.shortName.asString(), "code")

        return CallableClassDescriptor(imports, code, returnType)
    }

    private fun processCallKClass(callAnnotation: KSAnnotation, returnType: ClassName): TypeDescriptor {
        val className = callAnnotation.findArgument<String>("defaultValueClass")
            ?: throw CodeException.InvalidAnnotationArgument(callAnnotation.shortName.asString(), "defaultValue")

        return CallableKClassDescriptor(className, returnType)
    }

    private fun findImports(callAnnotation: KSAnnotation): List<String> {
        return callAnnotation.findArgument<List<String>>("imports")
            ?.let { imports ->
                val (valid, invalid) = imports.partition { it.isValidClass }

                if (invalid.isNotEmpty()) {
                    throw CodeException.NotAClass(invalid)
                }

                valid
            } ?: throw CodeException.InvalidAnnotationArgument(callAnnotation.shortName.asString(), "imports")
    }

    private val String.isValidClass: Boolean
        get() = try {
            Class.forName(this)
            true
        } catch (e: Exception) {
            false
        }
}
