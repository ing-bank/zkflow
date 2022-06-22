package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Surrogate.Companion.GENERATED_SURROGATE_SERIALIZER_POSTFIX
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.ksp.getSurrogateConverter
import com.ing.zkflow.ksp.singleAnnotation
import com.ing.zkflow.ksp.surrogateTargetType
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class SurrogateSerializerGenerator(private val codeGenerator: CodeGenerator) {

    fun generateSurrogateSerializers(surrogates: Sequence<KSClassDeclaration>) {
        surrogates.forEach { generateSurrogateSerializer(it) }
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
    private fun generateSurrogateSerializer(surrogate: KSClassDeclaration) {
        val zkpSurrogateAnnotation = surrogate.singleAnnotation<ZKPSurrogate>() ?: error(
            "Surrogate class `${surrogate.qualifiedName}` must be annotated with @${ZKPSurrogate::class.simpleName}."
        )

        val surrogateName = surrogate.asType(emptyList()).toTypeName()
        val converter = getSurrogateConverter(zkpSurrogateAnnotation)

        val packageName = surrogate.packageName.asString()
        val className = surrogate.simpleName.asString() + GENERATED_SURROGATE_SERIALIZER_POSTFIX
        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.objectBuilder("${surrogate.simpleName.asString()}${Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX}")
                    .superclass(
                        SurrogateSerializer::class
                            .asClassName()
                            .parameterizedBy(surrogate.surrogateTargetType.toTypeName(), surrogateName)
                    )
                    .addSuperclassConstructorParameter(
                        CodeBlock.of("%L.serializer(), { %L.from(it) }", surrogate, converter)
                    )
                    .build()
            )
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
                originatingKSFiles = listOfNotNull(surrogate.containingFile)
            )
    }
}
