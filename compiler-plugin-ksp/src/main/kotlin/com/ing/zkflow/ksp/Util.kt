@file:Suppress("TooManyFunctions")

package com.ing.zkflow.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.ZKPSurrogate
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.reflect.KClass

private val implementedInterfacesCache: MutableMap<KSClassDeclaration, Sequence<KSClassDeclaration>> = mutableMapOf()
private val implementedDirectInterfacesCache: MutableMap<KSClassDeclaration, Sequence<KSClassDeclaration>> = mutableMapOf()

fun KSClassDeclaration.getAllImplementedInterfaces(): Sequence<KSClassDeclaration> = implementedInterfacesCache.getOrPut(this) {
    getAllSuperTypes()
        .map { it.declaration }
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.INTERFACE }
}

fun KSClassDeclaration.getAllDirectlyImplementedInterfaces(): Sequence<KSClassDeclaration> =
    implementedDirectInterfacesCache.getOrPut(this) {
        superTypes
            .map { it.resolve().declaration }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.INTERFACE }
    }

/**
 * Returns true if the receiver implements the interfaceClass either directly or indirectly
 */
fun KSClassDeclaration.implementsInterface(interfaceClass: KClass<*>): Boolean =
    getAllImplementedInterfaces().any {
        it.qualifiedName?.asString() == interfaceClass.qualifiedName
    }

/**
 * Returns true only if the receiver implements the interfaceClass directly
 */
fun KSClassDeclaration.implementsInterfaceDirectly(interfaceClass: KClass<*>): Boolean =
    getAllDirectlyImplementedInterfaces().any {
        it.qualifiedName?.asString() == interfaceClass.qualifiedName
    }

/**
 * Returns true only if the receiver implements the interfaceClass directly
 */
fun KSClassDeclaration.implementsInterfaceDirectly(interfaceClass: KSClassDeclaration): Boolean =
    getAllDirectlyImplementedInterfaces().any {
        it.qualifiedName?.asString() == interfaceClass.qualifiedName?.asString()
    }

fun KSAnnotated.getAnnotationsByType(annotationKClass: KClass<out Annotation>): Sequence<KSAnnotation> {
    return this.annotations.filter {
        it.shortName.getShortName() == annotationKClass.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationKClass.qualifiedName
    }
}

fun getSurrogateConverter(zkpSurrogateAnnotation: KSAnnotation): TypeName {
    val zkpSurrogateArgument = zkpSurrogateAnnotation
        .arguments
        .single()
        .value as? KSType

    return zkpSurrogateArgument?.toTypeName()
        ?: error("Argument to `${ZKPSurrogate::class.qualifiedName}` must be a `KClass<${ConversionProvider::class.qualifiedName}>`")
}

val KSClassDeclaration.surrogateTargetType: RecursiveKSType
    get() = RecursiveKSType.from(getSurrogateTargetType())

data class RecursiveKSType(
    val ksClass: KSType,
    val arguments: List<RecursiveKSType>
) {
    fun toTypeName(): TypeName = if (arguments.isEmpty()) {
        ksClass.toClassName()
    } else {
        ksClass.toClassName().parameterizedBy(arguments.map { it.toTypeName() })
    }

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

inline fun <reified T : Any> KSClassDeclaration.singleAnnotation(): KSAnnotation? {
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

fun KSClassDeclaration.getSurrogateTargetClass(): KSClassDeclaration {
    val type = getSurrogateTargetType()
    return type
        .declaration as? KSClassDeclaration
        ?: error("$type is not a KSClassDeclaration.")
}

fun KSClassDeclaration.getSurrogateTargetType(): KSType {
    val surrogateArgument = superTypes.single {
        it.resolve().declaration.qualifiedName?.asString() == Surrogate::class.qualifiedName
    }.resolve().arguments.single()

    return surrogateArgument
        .type
        ?.resolve()
        ?: error("Could not resolve type of $surrogateArgument.")
}

fun Sequence<KSClassDeclaration>.filterConcreteClassesOrObjects(): Sequence<KSClassDeclaration> =
    filter { it.classKind in listOf(ClassKind.CLASS, ClassKind.OBJECT) && !it.isAbstract() }

/**
 * Surrogates are classes that are annotated with @ZKPSurrogate *and* implement [Surrogate].
 * Classes with only the annotation cause an error, classes only implementing the interface will be ignored.
 */
fun Resolver.getAllSurrogates(): Sequence<KSClassDeclaration> {
    val annotated = this.findClassesOrObjectsWithAnnotation(ZKPSurrogate::class)
    val implementing = annotated.filter { it.implementsInterface(Surrogate::class) }

    require(annotated.count() == implementing.count()) {
        "All @${ZKPSurrogate::class.simpleName}-annotated classes should implement the ${Surrogate::class.simpleName} interface. " +
            "The following do not: ${(annotated - implementing).map { it.qualifiedName?.asString() }.joinToString(", ")}"
    }
    return implementing
}

fun Resolver.findClassesOrObjectsWithAnnotation(annotationKClass: KClass<out Annotation>): Sequence<KSClassDeclaration> {
    return getSymbolsWithAnnotation(annotationKClass.qualifiedName!!)
        .filterIsInstance<KSClassDeclaration>()
        .filterConcreteClassesOrObjects()
}
