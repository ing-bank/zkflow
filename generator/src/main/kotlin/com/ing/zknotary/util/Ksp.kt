@file:Suppress("TooManyFunctions")
package com.ing.zknotary.util

import com.google.devtools.ksp.processing.KSPLogger

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.ing.zknotary.annotations.Sized
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlin.math.log
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.descriptors.PossiblyInnerType
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.STAR as KpStar

inline fun <reified T> Resolver.getClassDeclarationByName(): KSClassDeclaration {
    return getClassDeclarationByName(T::class.qualifiedName!!)
}

fun Resolver.getClassDeclarationByName(fqcn: String): KSClassDeclaration {
    return getClassDeclarationByName(getKSNameFromString(fqcn)) ?: error("Class '$fqcn' not found.")
}

fun KSClassDeclaration.asType() = asType(emptyList())

fun KSAnnotated.getAnnotationWithType(target: KSType): KSAnnotation {
    return findAnnotationWithType(target) ?: error("Annotation $target not found.")
}

fun KSAnnotated.hasAnnotation(target: KSType): Boolean {
    return findAnnotationWithType(target) != null
}

fun KSAnnotated.findAnnotationWithType(target: KSType): KSAnnotation? {
    return annotations.find { it.annotationType.resolve() == target }
}

inline fun <reified T> KSAnnotation.getMember(name: String): T {
    return arguments.find { it.name?.getShortName() == name }
        ?.value as? T
        ?: error("No member name found for $name.")
}

// this seems to fail
fun KSType.toTypeName(): TypeName {
    val type = when (declaration) {
        is KSClassDeclaration -> {
            (this as KSClassDeclaration).toTypeName(typeParameters.map { it.toTypeName() })
        }
        is KSTypeParameter -> {
            (this as KSTypeParameter).toTypeName()
        }
        else -> error("Unsupported type: $declaration")
    }

    val nullable = nullability == Nullability.NULLABLE

    return type.copy(nullable = nullable)
}

fun KSClassDeclaration.toTypeName(
    actualTypeArgs: List<TypeName> = typeParameters.map { it.toTypeName() }
): TypeName {
    val className = toClassName()
    return if (typeParameters.isNotEmpty()) {
        className.parameterizedBy(actualTypeArgs)
    } else {
        className
    }
}

fun KSClassDeclaration.toClassName(): ClassName {
    // Not ideal to be using bestGuess - https://github.com/android/kotlin/issues/23
    return ClassName.bestGuess(qualifiedName!!.asString())
}

fun KSTypeParameter.toTypeName(): TypeName {
    if (variance == Variance.STAR) return KpStar
    val typeVarName = name.getShortName()
    val typeVarBounds = bounds.map { it.toTypeName() }
    val typeVarVariance = when (variance) {
        Variance.COVARIANT -> KModifier.IN
        Variance.CONTRAVARIANT -> KModifier.OUT
        else -> null
    }
    return TypeVariableName(typeVarName, bounds = typeVarBounds, variance = typeVarVariance)
}

fun KSTypeReference.toTypeName(): TypeName {
    val type = resolve()
    return type.toTypeName()
}

fun KSAnnotation.toTypeName(): TypeName {
    return annotationType.resolve().toTypeName()
}

///
val KSType.typeName: TypeName
    get() {
        val primaryType = declaration.toString()
        val typeArgs = arguments.mapNotNull { it.type?.resolve() }

        val clazzName = ClassName(
            declaration.packageName.asString(),
            listOf(primaryType)
        )

        return if (typeArgs.isNotEmpty()) {
            clazzName.parameterizedBy(typeArgs.map { it.typeName })
        } else {
            clazzName
        }
    }


sealed class TypeConstruction(val definition: ClassName) {
    abstract fun debug(logger: KSPLogger)
    abstract val default: CodeBlock
    abstract val type: TypeName

    class Transient(
        definition: ClassName,
        val innerConstruction: TypeConstruction,
        val metadata: Metadata
    ) : TypeConstruction(definition) {
        override fun debug(logger: KSPLogger) {
            logger.error("(${(0..100).random()}) $definition")

            innerConstruction.debug(logger)
        }

        override val default: CodeBlock
            get() = CodeBlock.of(
                    metadata.pattern,
                    *metadata.args, innerConstruction.default as Any
                )

        override val type: TypeName
            get() = definition.parameterizedBy(innerConstruction.type)
    }

    class Final(
        definition: ClassName,
        override val default: CodeBlock
    ) : TypeConstruction(definition) {
        override fun debug(logger: KSPLogger) {
            logger.error("$definition : $default")
        }

        override val type: TypeName
            get() = definition
    }

    sealed class Metadata(val pattern: String) {
        abstract val args: Array<Any>
        class List_(val size: Int): Metadata("List( %L ) { %L }") {
            override val args: Array<Any>
                get() = arrayOf(size)
        }
    }
}

class PropertyConstruction(
    val name: String,
    val type: TypeName,
    val fromInstance: CodeBlock,
    val default: CodeBlock
) {
    fun debug(logger: KSPLogger) {
        logger.error("$name : $type")
        logger.error("$fromInstance")
        logger.error("$default")
    }
}

fun KSPropertyDeclaration.construct(original: String, logger: KSPLogger): PropertyConstruction {
    val name = simpleName.asString()
    val typeDef = type.resolve()
    val typeName = typeDef.declaration.toString()
    val typePackage = typeDef.declaration.packageName.asString()

    val propertyConstruction = if (typePackage.contains("collection", ignoreCase = true)) {
        when (typeName) {
            List::class.java.simpleName -> {
                val construction  = typeDef.construct() as TypeConstruction.Transient

                PropertyConstruction(
                    name = name,
                    type = construction.type,
                    fromInstance =
                        CodeBlock.of("%L.%L.extend(%L, %L)",
                            original,
                            name,
                            (construction.metadata as TypeConstruction.Metadata.List_).size,
                            construction.innerConstruction.default),
                    default = construction.default
                )
            }
            else -> error("Only Lists are supported")
        }
    } else {
        // Not a collection.
        val construction = typeDef.construct()

        PropertyConstruction(
            name = name,
            type = construction.type,
            fromInstance = CodeBlock.of("%L.%L", original, name),
            default = construction.default
        )
    }

    return propertyConstruction
}

fun KSType.construct(): TypeConstruction {
    val name = "$declaration"

    val construction = when (name) {
        // primitive types
        Int::class.simpleName -> {
            TypeConstruction.Final(
                ClassName(
                    declaration.packageName.asString(),
                    listOf(name)
                ),
                CodeBlock.builder().add("%L", 0).build()
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
            check(listType != null) { "List must have a type" }



            TypeConstruction.Transient(
                ClassName(
                        declaration.packageName.asString(),
                        listOf(name)
                    ),
                listType.construct(),
                TypeConstruction.Metadata.List_(size)
            )

            // val innerConstruction = listType.construct(logger, "$indent ")
            //
            // default.add("List($size) { %L }")
            //
            // val typeDefn = ClassName(
            //     declaration.packageName.asString(),
            //     listOf(name)
            // ).parameterizedBy(construction.typeDefinition)
            //
            // logger.error("$indent: param: ${construction.typeDefinition}")
            // logger.error("$indent: full: $typeName")
            //
            // typeDefn
        }
        else -> error("not supported: $name")
    }

    // val cb = default.build()

    // logger.error("$indent$typeName: $cb")

    // return TypeMeta(typeName, cb)
    return construction
}