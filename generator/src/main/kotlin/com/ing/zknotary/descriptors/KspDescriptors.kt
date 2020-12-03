package com.ing.zknotary.descriptors

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.descriptors.types.AnnotatedSizedClass
import com.ing.zknotary.descriptors.types.DefaultableClass
import com.ing.zknotary.descriptors.types.Int_
import com.ing.zknotary.descriptors.types.List_
import com.ing.zknotary.descriptors.types.Pair_
import com.ing.zknotary.descriptors.types.Triple_

fun KSPropertyDeclaration.describe(
    original: String,
    sizedClasses: List<KSClassDeclaration>
): PropertyDescriptor {
    val name = simpleName.asString()
    val typeDef = type.resolve()
    val typeName = typeDef.declaration.toString()

    var supported = false
    val errors = mutableListOf("Type $typeName is not supported\n")

    supported = supported || TypeDescriptor.supports(typeName)
    if (!supported) {
        errors += "Supported types:\n${TypeDescriptor.supported.joinToString(separator = ",\n")}"
    }

    // TODO have a better comment why it is ok to proceed
    supported = supported || sizedClasses.any { it.simpleName.asString() == typeName }
    if (!supported) {
        errors += "Class $typeName is not expected to have fixed length"
    }

    if (!supported) {
        error(errors.joinToString(separator = "\n"))
    }

    val descriptor = typeDef.describe()

    return PropertyDescriptor(
        name = name,
        type = descriptor.type,
        fromInstance = descriptor.toCodeBlock("$original.$name"),
        default = descriptor.default
    )
}

fun KSType.describe(useDefault: Boolean = false): TypeDescriptor =
    // TODO explicit defence against invariance
    // Invariant MUST hold:
    // This method must be be called on a type presentable as a fixed length instance.
    when ("$declaration") {
        // Primitive types
        Int::class.simpleName -> Int_(0, declaration)

        //
        // Compound types
        Pair::class.simpleName -> Pair_(
            arguments.subList(0, 2).map {
                val innerType = it.type?.resolve()
                require(innerType != null) { "Pair must have type arguments" }
                innerType.describe()
            }
        )

        Triple::class.simpleName -> Triple_(
            arguments.subList(0, 3).map {
                val innerType = it.type?.resolve()
                require(innerType != null) { "Pair must have type arguments" }
                innerType.describe()
            }
        )

        //
        // Collections
        List::class.simpleName -> List_.fromKSP(this)

        // Unknown type allowing fixed length representation.
        else -> {
            // Say, such class is called `SiClass`,
            // it is either expected either
            // 1. to have a sized version called SiClassSized,
            //    meaning it is expected to have a sized version called
            //    `KSClassDeclaration<SiClass>.sizedName`, or
            // 2. to have a default constructor, in this case,
            //    flag `useDefault` will be set to true
            val clazz = declaration as KSClassDeclaration
            if (useDefault) {
                DefaultableClass(clazz)
            } else {
                AnnotatedSizedClass(clazz)
            }
        }
    }

val KSClassDeclaration.sizedName: String
    get() = "${simpleName.asString()}Sized"
