package com.ing.zkflow.processors.serialization

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zkflow.Surrogate
import com.ing.zkflow.ksp.getSurrogateClassName
import com.ing.zkflow.ksp.getSurrogateSerializerClassName
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlinx.serialization.Serializable

internal sealed class SerializationFunctionalityGenerationTask(
    private val declaration: KSClassDeclaration
) {
    class Direct(declaration: KSClassDeclaration) : SerializationFunctionalityGenerationTask(declaration) {
        override val representation = declaration
        override fun declarationToSurrogateConstructor(surrogateClassName: ClassName): CodeBlock {
            val parameters = representation.primaryConstructor!!
                .parameters
                .joinToString(separator = ", ") { parameter ->
                    val name = parameter.name?.asString() ?: error("Cannot get a name of $parameter")
                    "$name = it.$name"
                }

            return CodeBlock.of("{ %T(%L) }", surrogateClassName, parameters)
        }

        override fun surrogateToDeclarationTemplate(): String =
            "return %T(%L)"
    }

    class Indirect(
        declaration: KSClassDeclaration,
        override val representation: KSClassDeclaration,
        private val declarationToRepresentationConverter: ClassName,
    ) : SerializationFunctionalityGenerationTask(declaration) {
        override fun declarationToSurrogateConstructor(surrogateClassName: ClassName): CodeBlock {
            val intermediate = "representation"

            val parameters = representation.primaryConstructor!!
                .parameters
                .joinToString(separator = ", ") { parameter ->
                    val name = parameter.name?.asString() ?: error("Cannot get a name of $parameter")
                    "$name = $intermediate.$name"
                }

            return CodeBlock.builder()
                .beginControlFlow("")
                .indent()
                .indent()
                .addStatement("val $intermediate = %T.from(it)", declarationToRepresentationConverter)
                .addStatement("%T(%L)", surrogateClassName, parameters)
                .endControlFlow()
                .unindent()
                .build()
        }

        override fun surrogateToDeclarationTemplate(): String =
            "return %T(%L).toOriginal()"
    }

    abstract val representation: KSClassDeclaration
    abstract fun declarationToSurrogateConstructor(surrogateClassName: ClassName): CodeBlock
    abstract fun surrogateToDeclarationTemplate(): String

    fun execute(): List<TypeSpec> {
        val surrogateClassName = declaration.getSurrogateClassName()
        val surrogate = generateSurrogate(surrogateClassName)

        val surrogateSerializerClassName = declaration.getSurrogateSerializerClassName()
        val surrogateSerializer = generateSurrogateSerializer(surrogateClassName, surrogateSerializerClassName)

        return listOf(surrogate, surrogateSerializer)
    }

    private fun generateSurrogate(surrogateClassName: ClassName): TypeSpec {
        val surrogateBuilder = TypeSpec.classBuilder(surrogateClassName)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("\"ClassName\"")
                    .build()
            )
            .addAnnotation(Serializable::class)
            .addModifiers(KModifier.DATA)
            .addSuperinterface(
                Surrogate::class
                    .asClassName()
                    .parameterizedBy(
                        declaration.toClassName()
                    )
            )

        val primaryConstructorBuilder = FunSpec.constructorBuilder()

        val parameters = mutableListOf<String>()

        representation.primaryConstructor!!.parameters.forEach { parameter ->
            val name = parameter.name?.asString() ?: error("Cannot get a name of $parameter")
            parameters += name

            val serializingHierarchy = parameter.getSerializingHierarchy()

            primaryConstructorBuilder.addParameter(name, serializingHierarchy.declaration)
            surrogateBuilder
                .addProperty(
                    PropertySpec.builder(name, serializingHierarchy.declaration)
                        .addAnnotation(
                            AnnotationSpec.builder(Serializable::class)
                                .addMember("with = %N::class", serializingHierarchy.definition)
                                .build()
                        )
                        .initializer(name)
                        .build()
                )
            serializingHierarchy.addTypesTo(surrogateBuilder)
        }

        return surrogateBuilder
            .primaryConstructor(primaryConstructorBuilder.build())
            .addFunction(
                FunSpec.builder("toOriginal")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(declaration.toClassName())
                    .addStatement(
                        surrogateToDeclarationTemplate(),
                        representation.toClassName(),
                        parameters.joinToString(separator = ", ")
                    )
                    .build()
            )
            .build()
    }

    private fun generateSurrogateSerializer(
        surrogateClassName: ClassName,
        surrogateSerializerClassName: ClassName
    ): TypeSpec {
        return TypeSpec.objectBuilder(surrogateSerializerClassName)
            .superclass(
                SurrogateSerializer::class
                    .asClassName()
                    .parameterizedBy(declaration.toClassName(), surrogateClassName)
            )
            .addSuperclassConstructorParameter(CodeBlock.of("%T.serializer()", surrogateClassName))
            .addSuperclassConstructorParameter(declarationToSurrogateConstructor(surrogateClassName))
            .build()
    }
}
