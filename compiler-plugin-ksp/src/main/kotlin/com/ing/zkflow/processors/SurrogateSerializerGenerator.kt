package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.ksp.getSingleArgumentOfSingleAnnotationByType
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

class SurrogateSerializerGenerator(private val codeGenerator: CodeGenerator) {
    /**
     * Generates:
     * - a kotlinx surrogate and,
     * - an appropriate serializer using converter defined in the accompanying ZKPSurrogate annotation.
     *
     * All fallable operations (`!!`, casts, single, etc.) are expected to be successful,
     * as the preconditions are verified elsewhere.
     */
    // fun processZKPSurrogateAnnotated(annotated: Sequence<KSClassDeclaration>) {
    // annotated.forEach {
    //     val surrogate = generateSurrogate(it)
    //     val converterClassName = (it.getAnnotationsByType(ZKPSurrogate::class).single().arguments.single().value as KSType).toClassName()
    //     generateSerializer(surrogate, converterClassName)
    // }
    // }

    /**
     * Generates:
     * - a kotlinx surrogate,
     * - a converter from an instance of a class to the surrogate
     * - an appropriate serializer using the above converter.
     *
     * All fallable operations (`!!`, casts, single, getting an annotation that must be present, etc.) are expected to be successful,
     * as the preconditions are verified elsewhere.
     */
    fun processZKPAnnotated(annotated: Sequence<KSClassDeclaration>) {
        annotated.forEach { declaration ->
            val surrogateClassName = getSurrogateClassName(declaration)
            val surrogate = generateSurrogateFromDeclaration(surrogateClassName, declaration)

            // val converterClassName = getConverterClassName(declaration)
            // val converter = generateConverter(from=surrogateClassName, to=)

            FileSpec.builder(surrogateClassName.packageName, surrogateClassName.simpleName)
                .addType(surrogate)
                .build()
                .writeTo(
                    codeGenerator = codeGenerator,
                    aggregating = false,
                    originatingKSFiles = listOfNotNull(declaration.containingFile)
                )
        }
    }

    // TODO more input is required
    // private fun generateConverter(declaration: KSClassDeclaration): ClassName {
    //     TODO("Not yet implemented")
    // }

    /**
     * It must be ensured that all constructor parameters of [declaration] are also fields (val/var).
     * All inner fields must be constructable from them.
     * Constructor parameters/fields are mapped to the surrogate.
     */
    private fun generateSurrogateFromDeclaration(surrogateClassName: ClassName, declaration: KSClassDeclaration): TypeSpec {
        val surrogateBuilder = TypeSpec.classBuilder(surrogateClassName)
            .addModifiers(KModifier.DATA)
            .addAnnotation(Serializable::class)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("\"ClassName\"")
                    .build()
            )
            .addSuperinterface(
                Surrogate::class
                    .asClassName()
                    .parameterizedBy(
                        declaration.toClassName()
                    )
            )

        val primaryConstructorBuilder = FunSpec.constructorBuilder()

        val parameters = mutableListOf<String>()

        declaration.primaryConstructor!!.parameters.forEach { parameter ->
            val name = parameter.name?.asString() ?: error("Cannot get a name of $parameter")
            parameters += name

            val serializingHierarchy = parameter.getSerializingHierarchy()

            primaryConstructorBuilder.addParameter(name, serializingHierarchy.declaration)
            surrogateBuilder
                .addProperty(
                    PropertySpec.builder(name, serializingHierarchy.declaration)
                        .addAnnotation(
                            AnnotationSpec.builder(Serializable::class)
                                .addMember("with = %N::class", serializingHierarchy.serializingObject)
                                .build()
                        )
                        .initializer(name)
                        .build()
                )
            serializingHierarchy.addTypesTo(surrogateBuilder)
        }

        return surrogateBuilder
            .primaryConstructor(
                primaryConstructorBuilder.build()
            )
            .addFunction(
                FunSpec.builder("toOriginal")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(declaration.toClassName())
                    .addStatement("return %T(%L)", declaration.toClassName(), parameters.joinToString(separator = ", "))
                    .build()
            )
            .build()
    }

    // TODO proper types for the parameters.
    // private fun generateSerializer(surrogate: CodeBlock, conversionProviderKClass: ClassName) {
    //
    // }

    private fun getSurrogateClassName(declaration: KSClassDeclaration): ClassName =
        ClassName(
            declaration.packageName.asString(),
            declaration.simpleName.asString() + Surrogate.GENERATED_SURROGATE_POSTFIX
        )
}

private sealed class SerializingHierarchy(
    val serializingObject: TypeSpec
) {
    class OfType(
        val rootType: ClassName,
        val inner: List<SerializingHierarchy>,
        serializingObject: TypeSpec,
    ) : SerializingHierarchy(serializingObject) {
        override val declaration: TypeName
            get() = rootTypeWithAnnotations(rootType.annotations + AnnotationSpec.builder(Contextual::class).build())

        override val type: TypeName
            get() = rootTypeWithAnnotations(emptyList())

        private fun rootTypeWithAnnotations(annotations: List<AnnotationSpec>): TypeName =
            if (inner.isEmpty()) {
                rootType.copy(annotations = annotations)
            } else {
                rootType
                    .parameterizedBy(inner.map { it.declaration })
                    .copy(annotations = annotations)
            }
    }

    class OfNullable(
        val inner: OfType,
        serializingObject: TypeSpec
    ) : SerializingHierarchy(serializingObject) {
        override val declaration: TypeName
            get() = inner.declaration.copy(nullable = true)
        override val type: TypeName
            get() = inner.type.copy(nullable = true)
    }

    abstract val declaration: TypeName
    abstract val type: TypeName
    fun addTypesTo(container: TypeSpec.Builder) {
        container.addType(serializingObject)
        when (this) {
            is OfType -> inner.forEach { it.addTypesTo(container) }
            is OfNullable -> inner.addTypesTo(container)
        }
    }
}

private fun KSValueParameter.getSerializingHierarchy(): SerializingHierarchy {
    val name = name?.asString() ?: error("Cannot get a name of $this")
    return type.resolve().getSerializingHierarchy(Tracker(name.capitalize()))
}

@Suppress("LongMethod")
private fun KSType.getSerializingHierarchy(tracker: Tracker, mustHaveDefault: Boolean = false): SerializingHierarchy {
    if (this.isMarkedNullable) {
        val inner = this.makeNotNullable().getSerializingHierarchy(tracker.next(), mustHaveDefault = true)
        require(inner is SerializingHierarchy.OfType) { "Type ${this.declaration} cannot be marked as nullable twice" }

        val typeSpec = TypeSpec.objectBuilder("$tracker")
            .addModifiers(KModifier.PRIVATE)
            .superclass(
                NullableSerializer::class
                    .asClassName()
                    .parameterizedBy(inner.type)
            )
            .addSuperclassConstructorParameter(CodeBlock.of("%N", inner.serializingObject))
            .build()

        return SerializingHierarchy.OfNullable(inner, typeSpec)
    }

    // Invariant:
    // Nullability has been stripped by now.

    // TODO this treatment must be done only for types whuch serializers have no default.
    //  Because we know all the serializers, we can look for defaults only in a very specific cases
    // if (mustHaveDefault) {
    //     val defaultProvider = (this as KSAnnotated).getSingleArgumentOfSingleAnnotationByType(Default::class)
    //         .toString()
    //         .dropLast("::class".length)
    //
    //     val inner = this.getSerializingObject(tracker.next(), mustHaveDefault = false)
    //
    //     val typeSpec = TypeSpec.objectBuilder("$tracker")
    //         .addModifiers(KModifier.PRIVATE)
    //         .superclass(
    //             SerializerWithDefault::class
    //                 .asClassName()
    //                 .parameterizedBy(inner.type)
    //         )
    //         .addSuperclassConstructorParameter(CodeBlock.of("%N, %L.default", inner.serializingObject, defaultProvider))
    //         .build()
    //
    //     return SerializingHierarchy.OfType(this.toClassName(), listOf(inner), typeSpec)
    // }

    val fqName = this.declaration.qualifiedName?.asString() ?: error("Cannot determine a fully qualified name of $declaration")

    return when (fqName) {
        Int::class.qualifiedName -> {
            val typeSpec = TypeSpec.objectBuilder("$tracker")
                .addModifiers(KModifier.PRIVATE)
                .superclass(
                    WrappedFixedLengthKSerializerWithDefault::class
                        .asClassName()
                        .parameterizedBy(this.toClassName())
                )
                .addSuperclassConstructorParameter("%T", IntSerializer::class)
                .build()

            SerializingHierarchy.OfType(this.toClassName(), listOf(), typeSpec)
        }

        Map::class.qualifiedName -> {
            val maxSize = (this as KSAnnotated).getSingleArgumentOfSingleAnnotationByType(Size::class)

            val keyType = this.arguments[0].type?.resolve() ?: error("Cannot resolve a type argument of Map")
            val keySerializingObject = keyType.getSerializingHierarchy(tracker.literal(0), mustHaveDefault = true)

            val valueType = this.arguments[1].type?.resolve() ?: error("Cannot resolve a type argument of Map")
            val valueSerializingObject = valueType.getSerializingHierarchy(tracker.literal(1), mustHaveDefault = true)

            val typeSpec = TypeSpec.objectBuilder("$tracker")
                .addModifiers(KModifier.PRIVATE)
                .superclass(
                    FixedLengthMapSerializer::class
                        .asClassName()
                        .parameterizedBy(keySerializingObject.type, valueSerializingObject.type)
                )
                .addSuperclassConstructorParameter(
                    CodeBlock.of("%L, %N, %N", maxSize, keySerializingObject.serializingObject, valueSerializingObject.serializingObject)
                )
                .build()

            SerializingHierarchy.OfType(this.toClassName(), listOf(keySerializingObject, valueSerializingObject), typeSpec)
        }

        else -> TODO("Unsupported type:  $fqName; ${if (mustHaveDefault) "Default value is required" else "Default value is NOT required"}")
    }
}

// class SurrogateSerializerGenerator(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) : ImplementationsProcessor<Surrogate<*>> {
//     override val interfaceClass = Surrogate::class
//     private val mapProviderInterface = SurrogateSerializerRegistryProvider::class
//
//     override fun process(implementations: List<ScopedDeclaration>): ServiceLoaderRegistration {
//         // Requirements:
//         // - Surrogate implementers _must_ be generics-free.
//         // - (a) Either annotated with @ZKP
//         // - (b) or with @ZKPSurrogate, in this case generate a serializer and registration.
//
//         // Generate surrogate serializers for all surrogates.
//         val generatedProviders = implementations.mapNotNull { impl ->
//             // Enforce no generics.
//             require(impl.declaration.typeParameters.isEmpty()) {
//                 "Surrogate implementers may not contain generics: `${impl.declaration}`"
//             }
//
//             // Enforce correctness of ZKP annotation.
//             // Presence of a ZKP annotation means there is no need to look for a ZKPSurrogate annotation,
//             // thus return null to evaluate the next implementation.
//             impl.declaration.singleAnnotation<ZKP>()?.let { return@mapNotNull null }
//
//             generateProvider(impl.declaration)
//         }
//
//         return ServiceLoaderRegistration(mapProviderInterface, generatedProviders)
//     }
//
//     private inline fun <reified T : Any> KSClassDeclaration.singleAnnotation(): KSAnnotation? {
//         val annotations = annotations.filter { ann ->
//             ann.shortName.asString() == T::class.simpleName!!
//         }.toList()
//
//         return when (annotations.size) {
//             0 -> null
//             1 -> annotations.single()
//             else -> error(
//                 """
//                 Annotations `${T::class.qualifiedName}` are not repeatable.
//                 Multiple annotations for `$qualifiedName` found.
//                 """.trimIndent()
//             )
//         }
//     }
//
//     /**
//      * For a given className and a ZKPSurrogate annotation,
//      * function will generate a surrogate serializer (example below) and return its class name in this [packageName].
//      *
//      * Surrogates for *, types with `in` or `out` will cause errors.
//      * ```
//      * object Amount_0 : SurrogateSerializer<Amount<IssuedTokenType>, AmountSurrogate_IssuedTokenType>(
//      *      AmountSurrogate_IssuedTokenType.serializer(),
//      *      { AmountConverter_IssuedTokenType.from(it) }
//      * )
//      * ```
//      */
//     private fun generateSurrogateSerializer(
//         surrogate: KSClassDeclaration,
//     ): TypeSpec {
//         // ZKPSurrogate annotation.
//         val zkpSurrogateAnnotation = surrogate.singleAnnotation<ZKPSurrogate>() ?: error(
//             """
//                 Surrogate class `${surrogate.qualifiedName}` must be annotated with
//                 either `${ZKP::class.qualifiedName}` or `${ZKPSurrogate::class.qualifiedName}`
//             """.trimIndent()
//         )
//
//         val surrogateName = surrogate.asType(emptyList()).toTypeName()
//         val converter = getSurrogateConverter(zkpSurrogateAnnotation)
//
//         return TypeSpec.objectBuilder(surrogate.surrogateSerializerClassName)
//             .superclass(
//                 SurrogateSerializer::class
//                     .asClassName()
//                     .parameterizedBy(surrogate.surrogateOriginalType.toTypeName(), surrogateName)
//             )
//             .addSuperclassConstructorParameter(
//                 CodeBlock.of("%L.serializer(), { %L.from(it) }", surrogate, converter)
//             )
//             .build()
//     }
//
//     private val KSClassDeclaration.surrogateSerializerClassName: String get() =
//         "${simpleName.asString()}${Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX}"
//
//     private fun getSurrogateConverter(zkpSurrogateAnnotation: KSAnnotation): TypeName {
//         val zkpSurrogateArgument = zkpSurrogateAnnotation
//             .arguments
//             .single()
//             .value as? KSType
//
//         return zkpSurrogateArgument?.toTypeName()
//             ?: error("Argument to `${ZKPSurrogate::class.qualifiedName}` must be a `KClass<${ConversionProvider::class.qualifiedName}>`")
//     }
//
//     private val KSClassDeclaration.surrogateOriginalType: RecursiveKSType get() {
//         val surrogateArgument = superTypes.single {
//             it.resolve().declaration.qualifiedName!!.asString() == Surrogate::class.qualifiedName
//         }.resolve().arguments.single()
//
//         val surrogateFor = surrogateArgument
//             .type
//             ?.resolve()
//             ?.let { RecursiveKSType.from(it) }
//
//         require(surrogateFor != null) {
//             "Cannot resolve `${surrogateArgument.toTypeName()}`"
//         }
//         return surrogateFor
//     }
//
//     private fun generateProvider(
//         surrogate: KSClassDeclaration,
//     ): String? {
//         // TODO We can now only register serializers for types that have no generics,
//         //      otherwise we shall, e.g., get
//         //      ```
//         //      Pair(net.corda.core.contracts.Amount<com.r3.cbdc.annotated.types.IssuedTokenType>::class,
//         //          com.ing.zkflow.serialization.infra.AmountSurrogate_IssuedTokenTypeSurrogateSerializer)
//         //      ```
//         //      which is invalid. Obviously.
//         val template = if (surrogate.surrogateOriginalType.templated()) {
//             "return %T(\n%L::class,\n%L,\n%L\n)"
//         } else {
//             logger.info("Not registering a serializer for ${surrogate.surrogateOriginalType}")
//             // "return /*\n%T(\n%L::class,\n%L,\n%L\n)*/"
//             null
//         }
//
//         val packageName = surrogate.packageName.asString()
//         val className = surrogate.simpleName.asString() + GENERATED_SERIALIZER_PROVIDER_POSTFIX
//         FileSpec.builder(packageName, className)
//             .addType(generateSurrogateSerializer(surrogate))
//             .guard(template != null) {
//                 addType(generateSerializerProvider(template!!, surrogate, className))
//             }
//             .build()
//             .writeTo(
//                 codeGenerator = codeGenerator,
//                 aggregating = false,
//                 originatingKSFiles = listOfNotNull(surrogate.containingFile)
//             )
//         return ifOrNull(template != null) { "$packageName.$className" }
//     }
//
//     private fun generateSerializerProvider(
//         template: String,
//         surrogate: KSClassDeclaration,
//         className: String,
//     ): TypeSpec {
//         val packageName = surrogate.packageName.asString()
//         val registration = buildCodeBlock {
//             addStatement(
//                 template,
//                 KClassSerializer::class,
//                 surrogate.surrogateOriginalType.toTypeName(),
//                 surrogate.surrogateSerializerClassName.hashCode(),
//                 "$packageName.${surrogate.surrogateSerializerClassName}",
//             )
//         }
//
//         return TypeSpec.classBuilder(className)
//             .addSuperinterface(mapProviderInterface)
//             .addFunction(
//                 FunSpec.builder("get")
//                     .addModifiers(KModifier.OVERRIDE)
//                     .returns(
//                         KClassSerializer::class.asClassName().parameterizedBy(
//                             Any::class.asClassName()
//                         )
//                     )
//                     .addCode(registration)
//                     .build()
//             )
//             .build()
//     }
//
//     private data class RecursiveKSType(
//         val ksClass: KSType,
//         val arguments: List<RecursiveKSType>
//     ) {
//         fun toTypeName(): TypeName = if (arguments.isEmpty()) {
//             ksClass.toClassName()
//         } else {
//             ksClass.toClassName().parameterizedBy(arguments.map { it.toTypeName() })
//         }
//
//         fun templated() = arguments.isEmpty()
//
//         companion object {
//             /**
//              * Recursively builds up a [RecursiveKSType] and verifies along the way that
//              * all types arguments are invariant.
//              */
//             fun from(ksType: KSType): RecursiveKSType = RecursiveKSType(
//                 ksType,
//                 ksType.arguments.map {
//                     require(it.variance == Variance.INVARIANT) {
//                         "Only invariant types are expected"
//                     }
//
//                     val typeRef = it.type
//                     require(typeRef != null) {
//                         "Type `${it.toTypeName()}` is not resolvable"
//                     }
//
//                     from(typeRef.resolve())
//                 }
//             )
//         }
//     }
// }
