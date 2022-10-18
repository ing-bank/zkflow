package com.ing.zkflow.common.zkp.metadata

import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

interface ZKIndexedTypedElement {
    val index: Int
    val type: KClass<out ContractState>
    fun mustBePrivate(): Boolean
    fun isPubliclyVisible() = !mustBePrivate()
}
