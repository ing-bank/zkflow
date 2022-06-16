package com.ing.zkflow.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
