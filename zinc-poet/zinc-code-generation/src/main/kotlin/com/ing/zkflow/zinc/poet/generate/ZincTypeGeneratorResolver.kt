package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zkflow.util.requireInstanceOf
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

class ZincTypeGeneratorResolver(
    private val zincTypeGenerator: ZincTypeGenerator
) : ZincTypeResolver {
    override fun zincTypeOf(kClass: KClass<*>): BflModule {
        return zincTypeGenerator.generate(kClass.serializer().descriptor).requireInstanceOf {
            "Expected ${kClass::qualifiedName} to be converted to a ${BflModule::class.qualifiedName}, but got ${it::class.qualifiedName}."
        }
    }
}
