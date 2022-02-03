package com.ing.zkflow.stateandcommanddata

import com.ing.zkflow.ksp.implementations.ImplementationsProcessor
import com.ing.zkflow.ksp.implementations.Registration
import com.ing.zkflow.ksp.implementations.ScopedDeclaration
import kotlin.reflect.KClass

class TransactionMetaDataProcessor<T : Any>(
    override val interfaceClass: KClass<T>,
) : ImplementationsProcessor<T> {

    override fun process(implementations: List<ScopedDeclaration>): Registration =
        Registration(interfaceClass, implementations.map { it.java.qualifiedName })
}
