package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.common.serialization.KClassSerializer
import com.ing.zkflow.common.serialization.SurrogateSerializerRegistryProvider
import com.ing.zkflow.ksp.implementations.ImplementationsProcessor
import com.ing.zkflow.ksp.implementations.ScopedDeclaration
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
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
import com.squareup.kotlinpoet.withIndent
import kotlin.math.absoluteValue
import kotlin.random.Random

class SurrogateSerializerGenerator(private val codeGenerator: CodeGenerator) : ImplementationsProcessor<Surrogate<*>> {
    override val interfaceClass = Surrogate::class
    private val mapProviderInterface = SurrogateSerializerRegistryProvider::class
    private val packageName = Surrogate.GENERATED_SURROGATE_SERIALIZER_PACKAGE_NAME

    override fun process(implementations: List<ScopedDeclaration>): ServiceLoaderRegistration {
        // Requirements:
        // - Surrogate implementers _must_ be generics-free.
        // - (a) Either annotated with @ZKP
        // - (b) or with @ZKPSurrogate, in this case generate a serializer and registration.

        // Generate surrogate serializers for all surrogates.
        val generatedSerializers = implementations.mapNotNull { impl ->
            // Enforce no generics.
            require(impl.declaration.typeParameters.isEmpty()) {
                "Surrogate implementers may not contain generics"
            }

            // Enforce correctness of ZKP annotation.
            // Presence of a ZKP annotation means there is no need to look for a ZKPSurrogate annotation,
            // thus return null to evaluate the next implementation.
            impl.declaration.singleAnnotation<ZKP>()?.let { return@mapNotNull null }

            // ZKPSurrogate annotation.
            val zkpSurrogateAnnotation = impl.declaration.singleAnnotation<ZKPSurrogate>() ?: error(
                """
                Surrogate class `${impl.declaration.qualifiedName}` must be annotated with
                either `${ZKP::class.qualifiedName}` or `${ZKPSurrogate::class.qualifiedName}`
                """.trimIndent()
            )

            generateSurrogateSerializer(impl.declaration, zkpSurrogateAnnotation)
        }

        val uid = Random.nextInt().absoluteValue
        val className = "${interfaceClass.simpleName}SerializerRegistryProvider$uid"
        generateProvider(className, generatedSerializers)

        return ServiceLoaderRegistration(mapProviderInterface, listOf("$packageName.$className"))
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
    private fun generateSurrogateSerializer(surrogate: KSClassDeclaration, zkpSurrogateAnnotation: KSAnnotation): GeneratedSurrogateSerializer {
        val className = "${surrogate.simpleName.asString()}${Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX}"

        val surrogateArgument = surrogate.superTypes.single {
            it.resolve().declaration.qualifiedName!!.asString() == Surrogate::class.qualifiedName
        }.resolve().arguments.single()

        val surrogateFor = surrogateArgument
            .type
            ?.resolve()
            ?.let { RecursiveKSType.from(it) }

        require(surrogateFor != null) {
            "Cannot resolve `${surrogateArgument.toTypeName()}`"
        }

        val surrogateName = surrogate.asType(emptyList()).toTypeName()

        val zkpSurrogateArgument = zkpSurrogateAnnotation
            .arguments
            .single()
            .value as? KSType

        val converter = zkpSurrogateArgument?.toTypeName()
            ?: error("Argument to `${ZKPSurrogate::class.qualifiedName}` must be a `KClass<${ConversionProvider::class.qualifiedName}>`")

        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.objectBuilder(className)
                    .superclass(
                        SurrogateSerializer::class
                            .asClassName()
                            .parameterizedBy(surrogateFor.toTypeName(), surrogateName)
                    )
                    .addSuperclassConstructorParameter(
                        CodeBlock.of("%L.serializer(), { %L.from(it) }", surrogate, converter)
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator = codeGenerator, aggregating = false)

        return GeneratedSurrogateSerializer(surrogateFor, className)
    }

    private fun generateProvider(
        className: String,
        generatedSurrogateSerializers: List<GeneratedSurrogateSerializer>
    ) {
        val registration = buildCodeBlock {
            add("return listOf(\n")
            withIndent {
                generatedSurrogateSerializers
                    .forEach { (surrogateFor, serializerFQName) ->
                        // TODO We can now only register serializers for types that have no generics,
                        //      otherwise we shall, e.g., get
                        //      ```
                        //      Pair(net.corda.core.contracts.Amount<com.r3.cbdc.annotated.types.IssuedTokenType>::class,
                        //          com.ing.zkflow.serialization.infra.AmountSurrogate_IssuedTokenTypeSurrogateSerializer)
                        //      ```
                        //      which is invalid. Obviously.
                        val template = if (surrogateFor.templated()) {
                            "%T(\n%L::class,\n%L,\n%L\n),"
                        } else {
                            "/*\n%T(\n%L::class,\n%L,\n%L\n),*/"
                        }

                        addStatement(
                            template,
                            KClassSerializer::class,
                            surrogateFor.toTypeName(),
                            serializerFQName.hashCode(),
                            serializerFQName,
                        )
                    }
            }
            add("\n)")
        }

        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(mapProviderInterface)
                    .addFunction(
                        FunSpec.builder("list")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(
                                List::class.asClassName().parameterizedBy(
                                    KClassSerializer::class.asClassName().parameterizedBy(
                                        Any::class.asClassName()
                                    )
                                )
                            )
                            .addCode(registration)
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator = codeGenerator, aggregating = false)
    }

    private data class GeneratedSurrogateSerializer(
        val surrogateFor: RecursiveKSType,
        val serializerFQName: String
    )

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
