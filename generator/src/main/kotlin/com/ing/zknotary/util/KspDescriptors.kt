package com.ing.zknotary.util

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.generator.log

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
        Int::class.simpleName -> TypeDescriptor.Int_(0, declaration)

        //
        // Compound types
        Pair::class.simpleName -> TypeDescriptor.Pair_(
            arguments.map {
                val innerType = it.type?.resolve()
                require(innerType != null) { "Pair must have type arguments" }
                innerType.describe()
            }
        )

        Triple::class.simpleName -> TypeDescriptor.Triple_(
            arguments.map {
                val innerType = it.type?.resolve()
                require(innerType != null) { "Pair must have type arguments" }
                innerType.describe()
            }
        )

        //
        // Collections
        List::class.simpleName -> {
            val listType = arguments.single().type?.resolve()
                ?: error( "List must have a type" )

            // Lists must be annotated with Sized.
            // TODO extract this into an ext function
            val sized = annotations.single {
                it.annotationType.toString().contains(
                    Sized::class.java.simpleName,
                    ignoreCase = true
                )
            }

            // TODO Too many hardcoded things: property names and their types.

            // Sized annotation must specify the size and,
            // perhaps, whether default is to be constructed.
            val size = sized.arguments.single {
                it.name?.getShortName() == "size"
            }.value as? Int ?: error("Int for `size` is expected")

            // TODO reconsider name shadowing
            @Suppress("NAME_SHADOWING")
            val useDefault = kotlin.run {
                val useDefault = sized.arguments.single {
                    it.name?.getShortName() == "useDefault"
                }.value as? Boolean ?: false

                if (!useDefault) {
                    return@run false
                }

                // Default for list element must be used.
                // Verify there is an empty constructor.

                // TODO split to nullable casting and further constructor evaluation
                (listType.declaration as? KSClassDeclaration)
                    ?.getConstructors()
                    ?.any { it.isPublic() && it.parameters.isEmpty() }
                    ?: error("Only classes can be instantiated with default")
            }

            TypeDescriptor.List_(size, listOf(listType.describe(useDefault)))
        }

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
                TypeDescriptor.DefaultableClass(clazz)
            } else {
                TypeDescriptor.SizedClass(clazz)
            }
        }
    }

val KSClassDeclaration.sizedName: String
    get() = "${simpleName.asString()}Sized"
