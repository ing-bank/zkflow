package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Surrogate.Companion.GENERATED_SERIALIZER_PROVIDER_POSTFIX
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.common.serialization.KClassSerializer
import com.ing.zkflow.common.serialization.SurrogateSerializerRegistryProvider
import com.ing.zkflow.ksp.implementations.ImplementationsProcessor
import com.ing.zkflow.ksp.implementations.ScopedDeclaration
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.util.guard
import com.ing.zkflow.util.ifOrNull
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class SurrogateSerializerGenerator(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) : ImplementationsProcessor<Surrogate<*>> {
    override val interfaceClass = Surrogate::class
    private val mapProviderInterface = SurrogateSerializerRegistryProvider::class

    override fun process(implementations: List<ScopedDeclaration>): ServiceLoaderRegistration {
        // Requirements:
        // - Surrogate implementers _must_ be generics-free.
        // - (a) Either annotated with @ZKP
        // - (b) or with @ZKPSurrogate, in this case generate a serializer and registration.

        // Generate surrogate serializers for all surrogates.
        val generatedProviders = implementations.mapNotNull { impl ->
            // Enforce no generics.
            require(impl.declaration.typeParameters.isEmpty()) {
                "Surrogate implementers may not contain generics: `${impl.declaration}`"
            }

            // Enforce correctness of ZKP annotation.
            // Presence of a ZKP annotation means there is no need to look for a ZKPSurrogate annotation,
            // thus return null to evaluate the next implementation.
            impl.declaration.singleAnnotation<ZKP>()?.let { return@mapNotNull null }

            generateProvider(impl.declaration)
        }

        return ServiceLoaderRegistration(mapProviderInterface, generatedProviders)
    }

    private inline fun <reified T : Any> KSClassDeclaration.singleAnnotation(): KSAnnotation? {
        val annotations = annotations.filter { ann ->
            ann.shortName.asString() == T::class.simpleName!!
        }.toList()

        return when (annotations.size) {
            0 -> null
            1 -> annotations.single()
            else -> error(
                """
                Annotations `${T::class.qualifiedName}` are not repeatable.
                Multiple annotations for `$qualifiedName` found.
                """.trimIndent()
            )
        }
    }

    /**
     * For a given className and a ZKPSurrogate annotation,
     * function will generate a surrogate serializer (example below) and return its class name in this [packageName].
     *
     * Surrogates for *, types with `in` or `out` will cause errors.
     * ```
     * object Amount_0 : SurrogateSerializer<Amount<IssuedTokenType>, AmountSurrogate_IssuedTokenType>(
     *      AmountSurrogate_IssuedTokenType.serializer(),
     *      { AmountConverter_IssuedTokenType.from(it) }
     * )
     * ```
     */
    private fun generateSurrogateSerializer(
        surrogate: KSClassDeclaration,
    ): TypeSpec {
        // ZKPSurrogate annotation.
        val zkpSurrogateAnnotation = surrogate.singleAnnotation<ZKPSurrogate>() ?: error(
            """
                Surrogate class `${surrogate.qualifiedName}` must be annotated with
                either `${ZKP::class.qualifiedName}` or `${ZKPSurrogate::class.qualifiedName}`
            """.trimIndent()
        )

        val surrogateName = surrogate.asType(emptyList()).toTypeName()
        val converter = getSurrogateConverter(zkpSurrogateAnnotation)

        return TypeSpec.objectBuilder(surrogate.surrogateSerializerClassName)
            .superclass(
                SurrogateSerializer::class
                    .asClassName()
                    .parameterizedBy(surrogate.surrogateOriginalType.toTypeName(), surrogateName)
            )
            .addSuperclassConstructorParameter(
                CodeBlock.of("%L.serializer(), { %L.from(it) }", surrogate, converter)
            )
            .build()
    }

    private val KSClassDeclaration.surrogateSerializerClassName: String get() =
        "${simpleName.asString()}${Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX}"

    private fun getSurrogateConverter(zkpSurrogateAnnotation: KSAnnotation): TypeName {
        val zkpSurrogateArgument = zkpSurrogateAnnotation
            .arguments
            .single()
            .value as? KSType

        return zkpSurrogateArgument?.toTypeName()
            ?: error("Argument to `${ZKPSurrogate::class.qualifiedName}` must be a `KClass<${ConversionProvider::class.qualifiedName}>`")
    }

    private val KSClassDeclaration.surrogateOriginalType: RecursiveKSType get() {
        val surrogateArgument = superTypes.single {
            it.resolve().declaration.qualifiedName!!.asString() == Surrogate::class.qualifiedName
        }.resolve().arguments.single()

        val surrogateFor = surrogateArgument
            .type
            ?.resolve()
            ?.let { RecursiveKSType.from(it) }

        require(surrogateFor != null) {
            "Cannot resolve `${surrogateArgument.toTypeName()}`"
        }
        return surrogateFor
    }

    private fun generateProvider(
        surrogate: KSClassDeclaration,
    ): String? {
        // TODO We can now only register serializers for types that have no generics,
        //      otherwise we shall, e.g., get
        //      ```
        //      Pair(net.corda.core.contracts.Amount<com.r3.cbdc.annotated.types.IssuedTokenType>::class,
        //          com.ing.zkflow.serialization.infra.AmountSurrogate_IssuedTokenTypeSurrogateSerializer)
        //      ```
        //      which is invalid. Obviously.
        val template = if (surrogate.surrogateOriginalType.templated()) {
            "return %T(\n%L::class,\n%L,\n%L\n)"
        } else {
            logger.info("Not registering a serializer for ${surrogate.surrogateOriginalType}")
            // "return /*\n%T(\n%L::class,\n%L,\n%L\n)*/"
            null
        }

        val packageName = surrogate.packageName.asString()
        val className = surrogate.simpleName.asString() + GENERATED_SERIALIZER_PROVIDER_POSTFIX
        FileSpec.builder(packageName, className)
            .addType(generateSurrogateSerializer(surrogate))
            .guard(template != null) {
                addType(generateSerializerProvider(template!!, surrogate, className))
            }
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
                originatingKSFiles = listOfNotNull(surrogate.containingFile)
            )
        return ifOrNull(template != null) { "$packageName.$className" }
    }

    private fun generateSerializerProvider(
        template: String,
        surrogate: KSClassDeclaration,
        className: String,
    ): TypeSpec {
        val packageName = surrogate.packageName.asString()
        val registration = buildCodeBlock {
            addStatement(
                template,
                KClassSerializer::class,
                surrogate.surrogateOriginalType.toTypeName(),
                surrogate.surrogateSerializerClassName.hashCode(),
                "$packageName.${surrogate.surrogateSerializerClassName}",
            )
        }

        return TypeSpec.classBuilder(className)
            .addSuperinterface(mapProviderInterface)
            .addFunction(
                FunSpec.builder("get")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(
                        KClassSerializer::class.asClassName().parameterizedBy(
                            Any::class.asClassName()
                        )
                    )
                    .addCode(registration)
                    .build()
            )
            .build()
    }

    private data class RecursiveKSType(
        val ksClass: KSType,
        val arguments: List<RecursiveKSType>
    ) {
        fun toTypeName(): TypeName = if (arguments.isEmpty()) {
            ksClass.toClassName()
        } else {
            ksClass.toClassName().parameterizedBy(arguments.map { it.toTypeName() })
        }

        fun templated() = arguments.isEmpty()

        companion object {
            /**
             * Recursively builds up a [RecursiveKSType] and verifies along the way that
             * all types arguments are invariant.
             */
            fun from(ksType: KSType): RecursiveKSType = RecursiveKSType(
                ksType,
                ksType.arguments.map {
                    require(it.variance == Variance.INVARIANT) {
                        "Only invariant types are expected"
                    }

                    val typeRef = it.type
                    require(typeRef != null) {
                        "Type `${it.toTypeName()}` is not resolvable"
                    }

                    from(typeRef.resolve())
                }
            )
        }
    }
}
