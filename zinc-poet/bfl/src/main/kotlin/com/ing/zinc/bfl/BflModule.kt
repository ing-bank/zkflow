package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflModule.Companion.getRegisteredMethodsFor
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincTypeDef
import java.util.Locale

/**
 * A [BflModule] is a [BflType] for which Zinc code can be generated.
 *
 * Each [BflModule] will be generated in its own file, a.k.a. module.
 *
 * This includes everything that extends [BflStruct] and [BflEnum].
 * This excludes [BflPrimitive] and [BflArray].
 */
interface BflModule : BflType {
    /**
     * The module name can be used to generate the filename, or module name for `use` statements.
     */
    fun getModuleName(): String = id.camelToSnakeCase()

    /**
     * Generate a [ZincFile] for this [BflModule].
     *
     * @param codeGenerationOptions
     */
    fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile

    /**
     * Generate methods that must be added to this struct or enum.
     *
     * NOTE: when overriding this method, include super first.
     */
    fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincFunction>

    companion object {
        private val methodsPerModuleRegistry: MutableMap<String, MutableMap<String, ZincFunction>> = mutableMapOf()

        /**
         * Registers a [method] for a module with id [moduleId].
         *
         * This is an extension mechanism to allow registering methods for [BflModule] instances that are not even
         * created yet, or will be automatically created, f.e. using reflection.
         */
        fun registerMethod(moduleId: String, method: ZincFunction) {
            val methodsRegistry = methodsPerModuleRegistry.getOrPut(moduleId) { mutableMapOf() }
            if (methodsRegistry[method.getName()] != null) {
                throw IllegalStateException("A method with name ${method.getName()} is already registered for $moduleId.")
            }
            methodsRegistry[method.getName()] = method
        }

        internal fun getRegisteredMethodsFor(moduleId: String): Collection<ZincFunction> {
            return methodsPerModuleRegistry.getOrDefault(moduleId, mutableMapOf()).values
        }
    }
}

/**
 * Return a list of additional methods that are registered via [registerMethod].
 */
internal fun BflModule.getRegisteredMethods(): Collection<ZincFunction> = getRegisteredMethodsFor(id)

/**
 * Return a name for a Constant holding the number of bits in the serialized form of this [BflModule].
 */
internal fun BflModule.getLengthConstant(): String = "${id.camelToSnakeCase().toUpperCase(Locale.getDefault())}_LENGTH"

/**
 * The type definition for the serialized form of this [BflModule].
 */
internal fun BflModule.getSerializedTypeDef(): ZincTypeDef = ZincTypeDef.zincTypeDef {
    name = "Serialized$id"
    type = ZincArray.zincArray {
        elementType = ZincPrimitive.Bool
        size = getLengthConstant()
    }
}
