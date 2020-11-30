package com.ing.zknotary.util

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.Sized

fun KSPropertyDeclaration.describe(original: String): PropertyDescriptor {
    val name = simpleName.asString()
    val typeDef = type.resolve()
    val typeName = typeDef.declaration.toString()

    if (!TypeDescriptor.supports(typeName)) {
        error("Type $typeName is not supported\n"+
            "Supported types:\n${TypeDescriptor.supported.joinToString(separator = ",\n")}")
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

        //
        // fail
        else -> error("not supported: $name")
    }

val KSClassDeclaration.sizedName: String
    get() = "${simpleName.asString()}Sized"