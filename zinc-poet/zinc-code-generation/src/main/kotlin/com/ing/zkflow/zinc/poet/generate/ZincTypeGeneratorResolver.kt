package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zkflow.util.requireInstanceOf
import kotlinx.serialization.serializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class ZincTypeGeneratorResolver(
    private val zincTypeGenerator: ZincTypeGenerator
) : ZincTypeResolver {
    private val knownTypesCache: MutableMap<KClass<*>, BflModule> = mutableMapOf()

    override fun zincTypeOf(kClass: KClass<*>): BflModule {
        return knownTypesCache.computeIfAbsent(kClass) {
            logger.info("Generating zinc type for $it")
            zincTypeGenerator.generate(kClass.serializer().descriptor).requireInstanceOf {
                "Expected ${kClass::qualifiedName} to be converted to a ${BflModule::class.qualifiedName}, but got ${it::class.qualifiedName}."
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ZincTypeGeneratorResolver::class.java)
    }
}
