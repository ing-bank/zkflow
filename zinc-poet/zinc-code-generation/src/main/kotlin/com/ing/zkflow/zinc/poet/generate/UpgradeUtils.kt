package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType.Companion.id
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.versioning.ZincUpgrade
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ZKProtectedComponent
import com.ing.zkflow.common.zkp.metadata.ZKReference
import com.ing.zkflow.util.require
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

fun generateUpgradeVerification(metadata: ResolvedZKCommandMetadata): ZincFile {
    metadata.inputs.require({ it.size == 1 }) {
        "Upgrade circuit MUST have a single input"
    }
    metadata.outputs.require({ it.size == 1 }) {
        "Upgrade circuit MUST have a single output"
    }
    return generateUpgradeVerification(
        metadata.commandSimpleName,
        metadata.inputs[0],
        metadata.outputs[0],
    )
}

private fun generateUpgradeVerification(
    commandName: String,
    original: ZKReference,
    upgraded: ZKProtectedComponent,
): ZincFile {
    val zincTypeResolver = ZincTypeGeneratorResolver(ZincTypeGenerator)
    val originalModule = zincTypeResolver.zincTypeOf(original.type)
    val upgradedModule = zincTypeResolver.zincTypeOf(upgraded.type)
    return zincFile {
        "CommandContext".let {
            val moduleName = "module_" + it.camelToSnakeCase()
            mod { module = moduleName }
            use { path = "$moduleName::$it" }
            newLine()
        }
        listOf(originalModule, upgradedModule).forEach {
            mod { module = it.getModuleName() }
            use { path = "${it.getModuleName()}::${it.typeName()}" }
            newLine()
        }
        function {
            name = "verify"
            parameter {
                name = "ctx"
                type = id("CommandContext")
            }
            returnType = ZincPrimitive.Unit
            body = """
                let input: ${originalModule.typeName()} = ctx.inputs.${originalModule.typeName().camelToSnakeCase()}_${original.index}.data;
                let output: ${upgradedModule.typeName()} = ctx.outputs.${upgradedModule.typeName().camelToSnakeCase()}_${upgraded.index}.data;

                assert!(output.equals(${upgradedModule.typeName()}::upgrade_from(input)), "[$commandName] Not a valid upgrade from ${originalModule.typeName()} to ${upgradedModule.typeName()}.");
            """.trimIndent()
        }
    }
}
