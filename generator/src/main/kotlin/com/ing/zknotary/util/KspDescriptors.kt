package com.ing.zknotary.util

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.annotations.WrappedList
import com.squareup.kotlinpoet.ClassName

fun KSPropertyDeclaration.describe(original: String, logger: KSPLogger): PropertyDescriptor {
    val name = simpleName.asString()
    val typeDef = type.resolve()
    val typeName = typeDef.declaration.toString()
    val typePackage = typeDef.declaration.packageName.asString()

    // TODO does typePackage cover all collections?
    // TODO
    // 1 list of processable type (includes primitive types and collections AT LEAST)
    // 2 filter and throw exception if necessary
    // 3 typeDef.construct()

    val propertyConstruction = if (typePackage.contains("collection", ignoreCase = true)) {
        when (typeName) {
            List::class.java.simpleName -> {
                val construction  = typeDef.describe()

                PropertyDescriptor(
                    name = name,
                    type = construction.type,
                    fromInstance = construction.toCodeBlock("$original.$name"),
                    default = construction.default
                )
            }
            else -> error("Only Lists are supported")
        }
    } else {
        // Not a collection.
        val construction = typeDef.describe()

        PropertyDescriptor(
            name = name,
            type = construction.type,
            fromInstance = construction.toCodeBlock("$original.$name"),
            default = construction.default
        )
    }

    return propertyConstruction
}

fun KSType.describe(): TypeDescriptor {
    val construction = when (val name = "$declaration") {
        // primitive types
        Int::class.simpleName -> {
            TypeDescriptor.Trailing(
                ClassName(
                    declaration.packageName.asString(),
                    listOf(name)
                ),
                TypeKind.Int_(0)
            )
        }
        //
        // collections
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

            TypeDescriptor.Transient(
                ClassName(
                    WrappedList::class.java.`package`.name,
                    listOf(WrappedList::class.simpleName!!)
                ),
                listType.describe(),
                TypeKind.WrappedList_(size)
            )
        }
        else -> error("not supported: $name")
    }

    return construction
}