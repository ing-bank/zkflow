package com.ing.zkflow.common.versioning

import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

fun generateUpgradeCommandClassName(
    previous: KClass<out ContractState>,
    current: KClass<out ContractState>,
    isPrivate: Boolean
) = generateUpgradeCommandClassName(
    previous.simpleName ?: error("Can't generate upgrade command for $previous: it has no simpleName"),
    current.simpleName ?: error("Can't generate upgrade command for $previous: it has no simpleName"),
    isPrivate
)

fun generateUpgradeCommandClassName(
    previousSimpleName: String,
    currentSimpleName: String,
    isPrivate: Boolean
): String {
    val privateOrAnyInput = if (isPrivate) "Private" else "Any"
    val privateOrPublicOutput = if (isPrivate) "Private" else "Public"
    return "Upgrade$privateOrAnyInput${previousSimpleName}To$privateOrPublicOutput$currentSimpleName"
}
