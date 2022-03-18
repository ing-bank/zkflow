package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
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
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.withIndent
import kotlin.math.absoluteValue
import kotlin.random.Random

class SurrogateSerializerGenerator(private val codeGenerator: CodeGenerator) : ImplementationsProcessor<Surrogate<*>> {
    override val interfaceClass = Surrogate::class
    private val mapProviderInterface = SurrogateSerializerRegistryProvider::class
    private val packageName = "com.ing.zkflow.serialization.infra"

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
     * function will generate a surrogate serializer (example below) and return its fully qualified class name.
     *
     * ```
     * object Amount_0 : SurrogateSerializer<Amount<IssuedTokenType>, AmountSurrogate_IssuedTokenType>(
     *      AmountSurrogate_IssuedTokenType.serializer(),
     *      { AmountConverter_IssuedTokenType.from(it) }
     * )
     * ```
     */
    private fun generateSurrogateSerializer(surrogate: KSClassDeclaration, zkpSurrogateAnnotation: KSAnnotation): GeneratedSurrogateSerializer {
        val className = "${surrogate.simpleName.asString()}SurrogateSerializer"

        val surrogateFor = surrogate.superTypes.single {
            it.resolve().declaration.qualifiedName!!.asString() == Surrogate::class.qualifiedName
        }.resolve().arguments.single().toTypeName()

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
                            .parameterizedBy(surrogateFor, surrogateName)
                    )
                    .addSuperclassConstructorParameter(
                        CodeBlock.of("%L.serializer(), { %L.from(it) }", surrogate, converter)
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator = codeGenerator, aggregating = false)

        return GeneratedSurrogateSerializer(surrogateFor, "$packageName.$className")
    }

    private fun generateProvider(
        className: String,
        generatedSurrogateSerializers: List<GeneratedSurrogateSerializer>
    ) {
        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(mapProviderInterface)
                    .addFunction(
                        FunSpec.builder("list")
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                buildCodeBlock {
                                    add("return listOf(")
                                    withIndent {
                                        generatedSurrogateSerializers.forEach { (surrogateFor, serializerFQName) ->
                                            addStatement(
                                                "%T(%L::class, %L),",
                                                Pair::class,
                                                surrogateFor,
                                                serializerFQName,
                                            )
                                        }
                                    }
                                    add(")")
                                }
                            )
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator = codeGenerator, aggregating = false)
    }

    private data class GeneratedSurrogateSerializer(
        val surrogateFor: TypeName,
        val serializerFQName: String
    )
}
