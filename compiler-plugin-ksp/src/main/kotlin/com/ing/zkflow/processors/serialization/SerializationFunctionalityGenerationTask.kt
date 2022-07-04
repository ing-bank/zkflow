package com.ing.zkflow.processors.serialization

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zkflow.Surrogate
import com.ing.zkflow.ksp.getSurrogateClassName
import com.ing.zkflow.ksp.getSurrogateSerializerClassName
import com.ing.zkflow.ksp.getSurrogateTargetType
import com.ing.zkflow.ksp.toCleanTypeName
import com.ing.zkflow.ksp.toContextualTypeName
import com.ing.zkflow.processors.serialization.hierarchy.getSerializingHierarchy
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlinx.serialization.Serializable

internal sealed class SerializationFunctionalityGenerationTask(
    internal val declaration: KSClassDeclaration
) {
    class Direct(declaration: KSClassDeclaration) : SerializationFunctionalityGenerationTask(declaration) {
        override val representation = declaration
        override fun declarationToSurrogateConstructor(surrogateClassName: ClassName): CodeBlock {
            val parameters = if (isEnum()) {
                "ordinal = it.ordinal"
            } else {
                representation.primaryConstructor!!
                    .parameters
                    .joinToString(separator = ", ") { parameter ->
                        val name = parameter.name?.asString() ?: error("Cannot get a name of $parameter")
                        "$name = it.$name"
                    }
            }

            return CodeBlock.of("{ %T(%L) }", surrogateClassName, parameters)
        }

        override fun surrogateToDeclarationTemplate(): String =
            "return %T(%L)"

        // ZKP-annotated classes may not have type parameters,
        // so it is safe to return declaration as a class name without inspecting deeper levels.
        override val surrogateTarget: TypeName
            get() = declaration.toClassName()
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

        override val surrogateTarget: TypeName
            get() = representation.getSurrogateTargetType().toCleanTypeName()
    }

    abstract val representation: KSClassDeclaration
    abstract val surrogateTarget: TypeName
    abstract fun declarationToSurrogateConstructor(surrogateClassName: ClassName): CodeBlock
    abstract fun surrogateToDeclarationTemplate(): String

    fun execute(): List<TypeSpec> {
        val surrogateClassName = representation.getSurrogateClassName()
        val surrogate = generateSurrogate(surrogateClassName)

        val surrogateSerializerClassName = representation.getSurrogateSerializerClassName()
        val surrogateSerializer = generateSurrogateSerializer(surrogateClassName, surrogateSerializerClassName)

        return listOf(surrogate, surrogateSerializer)
    }

    private fun generateSurrogate(surrogateClassName: ClassName): TypeSpec {
        val surrogateBuilder = TypeSpec.classBuilder(surrogateClassName)
            .addAnnotation(Serializable::class)
            .addSuperinterface(
                Surrogate::class
                    .asClassName()
                    .parameterizedBy(surrogateTarget)
            )

        return if (isEnum()) {
            generateSurrogateForEnum(surrogateBuilder)
        } else {
            generateSurrogateForClass(surrogateBuilder)
        }
    }

    private fun generateSurrogateForEnum(surrogateBuilder: TypeSpec.Builder): TypeSpec {
        val name = "ordinal"

        val propertySerializer = TypeSpec.objectBuilder("${name.capitalize()}_0")
            .addModifiers(KModifier.PRIVATE)
            .superclass(
                WrappedFixedLengthKSerializerWithDefault::class
                    .asClassName()
                    .parameterizedBy(Int::class.asClassName())
            )
            .addSuperclassConstructorParameter("%T", IntSerializer::class)
            .build()

        return surrogateBuilder
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(name, Int::class)
                    .build()
            )
            .addProperty(
                PropertySpec.builder(name, Int::class)
                    .addAnnotation(
                        AnnotationSpec.builder(Serializable::class)
                            .addMember("with = %N::class", propertySerializer)
                            .build()
                    )
                    .initializer(name)
                    .build()
            )
            .addType(propertySerializer)
            .addFunction(
                FunSpec.builder("toOriginal")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(declaration.toClassName())
                    .addStatement("return %T.values().first { it.$name == $name }", declaration.toClassName())
                    .build()
            )
            .build()
    }

    private fun generateSurrogateForClass(surrogateBuilder: TypeSpec.Builder): TypeSpec {
        val primaryConstructorBuilder = FunSpec.constructorBuilder()

        val parameters = mutableListOf<String>()

        representation.primaryConstructor!!.parameters.forEach { parameter ->
            val name = parameter.name?.asString() ?: error("Cannot get a name of $parameter")
            parameters += name

            val serializingHierarchy = parameter.getSerializingHierarchy()
            val declaration = parameter.type.resolve().toContextualTypeName()

            primaryConstructorBuilder.addParameter(name, declaration)
            surrogateBuilder
                .addProperty(
                    PropertySpec.builder(name, declaration)
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
                    .returns(surrogateTarget)
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
                    .parameterizedBy(surrogateTarget, surrogateClassName)
            )
            .addSuperclassConstructorParameter(CodeBlock.of("%T.serializer()", surrogateClassName))
            .addSuperclassConstructorParameter(declarationToSurrogateConstructor(surrogateClassName))
            .build()
    }

    internal fun isEnum() = declaration.classKind == ClassKind.ENUM_CLASS
}
