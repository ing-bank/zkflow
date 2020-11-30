package com.ing.zknotary.util

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

fun KSType.describe(): TypeDescriptor =
    // Invariant MUST hold:
    // This method must be be called on a type presentable as a fixed length instance.
    when (val name = "$declaration") {
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
            // Lists must be annotated with Sized.
            val sized = annotations.single {
                it.annotationType.toString().contains(
                    Sized::class.java.simpleName,
                    ignoreCase = true
                )
            }

            // Sized annotation must specify the size.
            // TODO Too many hardcoded things: "size" and Int
            val size = sized.arguments.single {
                it.name?.getShortName() == "size"
            }.value as? Int ?: error("Int size is expected")

            val listType = arguments.single().type?.resolve()
            require(listType != null) { "List must have a type" }

            TypeDescriptor.List_(size, listOf(listType.describe()))
        }

        // Unknown type allowing fixed length representation.
        else -> {
            // Say, such class in called `SiClass`,
            // meaning it is expected to have it sized version called
            // `KSClassDeclaration<SiClass>.sizedName`

            TypeDescriptor.SizedClass(
                declaration as KSClassDeclaration
            )
        }
    }

val KSClassDeclaration.sizedName: String
    get() = "${simpleName.asString()}Sized"