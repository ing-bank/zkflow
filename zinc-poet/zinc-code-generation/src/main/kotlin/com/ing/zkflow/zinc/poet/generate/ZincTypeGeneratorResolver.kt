package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.poet.ZincFunction.Companion.zincFunction
import com.ing.zinc.poet.ZincType.Companion.id
import com.ing.zinc.poet.ZincType.Self
import com.ing.zkflow.serialization.getSerialDescriptor
import com.ing.zkflow.serialization.zincTypeName
import com.ing.zkflow.util.requireInstanceOf
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

            zincTypeGenerator.generate(kClass.getSerialDescriptor()).requireInstanceOf<BflModule> {
                "Expected ${kClass::qualifiedName} to be converted to a ${BflModule::class.qualifiedName}, but got ${it::class.qualifiedName}."
            }.let { module ->
                if (module is BflStruct) {
                    findUpgradeParameters(kClass)?.let { upgradeParameters ->
                        module.copyWithAdditionalFunctionsAndImports(
                            listOf(generateUpgradeFunction(upgradeParameters)),
                            listOf(zincTypeOf(upgradeParameters.originalKClass))
                        )
                    } ?: module
                } else {
                    module
                }
            }
        }
    }

    private fun generateUpgradeFunction(
        it: UpgradeParameters
    ) = zincFunction {
        name = "upgrade_from"
        returnType = Self
        parameter {
            name = it.zincUpgradeParameterName
            type = id(it.originalKClass.zincTypeName)
        }
        body = it.zincUpgradeBody
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ZincTypeGeneratorResolver::class.java)
    }
}
