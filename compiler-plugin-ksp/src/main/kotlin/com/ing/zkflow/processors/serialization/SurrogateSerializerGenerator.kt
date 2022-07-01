package com.ing.zkflow.processors.serialization

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.ksp.getSerializationFunctionalityLocation
import com.ing.zkflow.ksp.getSingleArgumentOfNonRepeatableAnnotationByType
import com.ing.zkflow.ksp.getSurrogateTargetClass
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class SurrogateSerializerGenerator(private val codeGenerator: CodeGenerator) {
    /**
     * Generates:
     * - a kotlinx surrogate and,
     * - an appropriate serializer using converter defined in the accompanying ZKPSurrogate annotation.
     *
     * All fallable operations (`!!`, casts, single, etc.) are expected to be successful,
     * as the preconditions are verified elsewhere.
     */
    fun processZKPSurrogateAnnotated(annotated: Sequence<KSClassDeclaration>) {
        annotated.forEach { representation ->
            val declaration = representation.getSurrogateTargetClass()
            val converterClassName =
                (representation.getSingleArgumentOfNonRepeatableAnnotationByType(ZKPSurrogate::class) as KSType).toClassName()

            val typeSpecs = SerializationFunctionalityGenerationTask.Indirect(
                declaration,
                representation,
                converterClassName
            ).execute()

            emit(
                location = representation.getSerializationFunctionalityLocation(),
                typeSpecs = typeSpecs,
                dependencies = listOf(representation)
            )
        }
    }

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
            val typeSpecs = SerializationFunctionalityGenerationTask.Direct(declaration).execute()

            emit(
                location = declaration.getSerializationFunctionalityLocation(),
                typeSpecs = typeSpecs,
                dependencies = listOf(declaration)
            )
        }
    }

    /**
     * Write out given [typeSpecs] at given [location].
     */
    private fun emit(location: ClassName, typeSpecs: List<TypeSpec>, dependencies: List<KSClassDeclaration>) {
        val fileSpecBuilder = FileSpec.builder(location.packageName, location.simpleName)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("\"ClassName\"")
                    .addMember("\"DEPRECATION\"")
                    .build()
            )

        typeSpecs.forEach { fileSpecBuilder.addType(it) }

        fileSpecBuilder
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
                originatingKSFiles = dependencies.mapNotNull { it.containingFile }
            )
    }
}
