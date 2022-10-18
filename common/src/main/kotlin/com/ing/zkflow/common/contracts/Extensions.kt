package com.ing.zkflow.common.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

val ContractClassName.packageName: String?
    get() {
        val i = lastIndexOf('.')
        return if (i != -1) {
            substring(0, i)
        } else {
            null
        }
    }

/**
 * Obtain the typename of the required [ontractClass] associated with the target [ContractState], using the
 * [BelongsToContract] annotation by default, but falling through to checking the state's enclosing class if there is
 * one and it inherits from [Contract].
 */
val KClass<out ContractState>.requiredContractClassName: String?
    get() {
        val annotation = java.getAnnotation(BelongsToContract::class.java)
        val enclosingClass = java.enclosingClass

        return if (annotation != null) {
            annotation.value.java.typeName
        } else if (enclosingClass != null && Contract::class.java.isAssignableFrom(enclosingClass)) {
            enclosingClass.typeName
        } else {
            null
        }
    }
