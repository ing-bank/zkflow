package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFile.Companion.zincFile
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType.Companion.id
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.versioning.ZincUpgrade
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ZKProtectedComponent
import com.ing.zkflow.common.zkp.metadata.ZKReference
import com.ing.zkflow.util.require
import com.ing.zkflow.util.requireNotNull
import com.ing.zkflow.util.tryGetKClass
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

fun KClass<*>.isVersioned(): Boolean = tryFindFamilyKClass() != null

fun KClass<*>.tryFindFamilyKClass(): KClass<out Any>? = superclasses.singleOrNull {
    it.superclasses.contains(VersionedContractStateGroup::class)
}

data class UpgradeParameters(
    val originalKClass: KClass<out Any>,
    val zincUpgradeBody: String,
    val zincUpgradeParameterName: String,
)

fun findAdditionalChecks(stateKClass: KClass<out Any>): String = if (stateKClass.isVersioned()) {
    stateKClass.constructors
        .singleOrNull {
            it.parameters.size == 1 &&
                (it.parameters.single().type.classifier as KClass<*>).isSubclassOf(stateKClass.tryFindFamilyKClass()!!)
        }
        .requireNotNull { "No upgrade constructor found on $stateKClass." }
        .getZincUpgradeAdditionalChecks()
} else {
    ""
}

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

/**
 * This function requires that the [ZincUpgrade] annotation exists and throws an [IllegalStateException] if it doesn't.
 */
private fun KFunction<Any>.getZincUpgradeBody(): String = this.findAnnotation<ZincUpgrade>()?.upgrade
    ?: throw IllegalStateException("Upgrade constructor MUST be annotated with ${ZincUpgrade::class.simpleName}")

private fun KFunction<Any>.getZincUpgradeAdditionalChecks(): String = this.findAnnotation<ZincUpgrade>()?.additionalChecks?.trimIndent()
    ?: throw IllegalStateException("Upgrade constructor '$this' MUST be annotated with ${ZincUpgrade::class.simpleName}")

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
    val additionalChecks = findAdditionalChecks(upgraded.type)
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
                let output: ${upgradedModule.typeName()} = ctx.outputs.${
            upgradedModule.typeName().camelToSnakeCase()
            }_${upgraded.index}.data;

                assert!(output.equals(${upgradedModule.typeName()}::upgrade_from(input)), "[$commandName] Not a valid upgrade from ${originalModule.typeName()} to ${upgradedModule.typeName()}.");
                $additionalChecks
            """.trimIndent()
        }
    }
}
