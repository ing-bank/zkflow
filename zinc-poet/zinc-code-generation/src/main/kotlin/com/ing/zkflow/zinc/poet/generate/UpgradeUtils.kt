package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.versioning.ZincUpgrade
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation

fun KClass<*>.isVersioned(): Boolean = allSuperclasses.contains(Versioned::class)

fun KClass<*>.tryFindFamilyKClass(): KClass<out Any>? = allSuperclasses.singleOrNull {
    it.isVersioned()
}

data class UpgradeParameters(
    val originalKClass: KClass<out Any>,
    val zincUpgradeBody: String,
    val zincUpgradeParameterName: String,
)

fun findUpgradeParameters(stateKClass: KClass<out Any>): UpgradeParameters? = if (stateKClass.isVersioned()) {
    stateKClass.constructors
        .filter { it.parameters.size == 1 } // consider only single argument constructors
        .mapNotNull { it.tryGetUpgradeParameters(stateKClass) }
        .singleOrNull()
} else {
    null
}

private fun KFunction<Any>.tryGetUpgradeParameters(upgradedKClass: KClass<out Any>): UpgradeParameters? =
    tryGetFirstArgumentKClass()?.let { originalKClass ->
        if (originalKClass.isVersioned() &&
            originalKClass.tryFindFamilyKClass()?.let { it == upgradedKClass.tryFindFamilyKClass() } == true
        ) {
            UpgradeParameters(
                originalKClass,
                getZincUpgradeBody(),
                this.parameters[0].name?.camelToSnakeCase() ?: "previous"
            )
        } else {
            null
        }
    }

private fun KFunction<Any>.tryGetFirstArgumentKClass(): KClass<out Any>? = try {
    "${parameters[0].type}".tryGetKClass()
} catch (e: ClassNotFoundException) {
    null
}

private fun String.tryGetKClass(): KClass<out Any>? = classNamePermutations().mapNotNull {
    try {
        Class.forName(it).kotlin
    } catch (e: ClassNotFoundException) {
        null
    }
}.singleOrNull()

private fun String.classNamePermutations(): List<String> = split(".").fold(emptyList()) { acc, part ->
    if (acc.isEmpty()) {
        listOf(part)
    } else {
        acc.map { "$it.$part" } + acc.map { "$it\$$part" }
    }
}

/**
 * This function requires that the [ZincUpgrade] annotation exists and throws an [IllegalStateException] if it doesn't.
 */
private fun KFunction<Any>.getZincUpgradeBody(): String = this.findAnnotation<ZincUpgrade>()?.body
    ?: throw IllegalStateException("Upgrade constructor MUST be annotated with ${ZincUpgrade::class.simpleName}")
