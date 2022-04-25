package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflModule.Companion.getRegisteredMethodsFor
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincInvokeable
import com.ing.zinc.poet.ZincMod
import com.ing.zinc.poet.ZincMod.Companion.zincMod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincTypeDef
import com.ing.zinc.poet.ZincTypeDef.Companion.zincTypeDef
import com.ing.zinc.poet.ZincUse
import com.ing.zinc.poet.ZincUse.Companion.zincUse
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
    fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincInvokeable>

    companion object {
        private val methodsPerModuleRegistry: MutableMap<String, MutableMap<String, ZincInvokeable>> = mutableMapOf()

        /**
         * Registers a [method] for a module with id [moduleId].
         *
         * This is an extension mechanism to allow registering methods for [BflModule] instances that are not even
         * created yet, or will be automatically created, e.g. using reflection.
         */
        fun registerMethod(moduleId: String, method: ZincInvokeable) {
            val methodsRegistry = methodsPerModuleRegistry.getOrPut(moduleId) { mutableMapOf() }
            if (methodsRegistry[method.getName()] != null) {
                throw IllegalStateException("A method with name ${method.getName()} is already registered for $moduleId.")
            }
            methodsRegistry[method.getName()] = method
        }

        internal fun getRegisteredMethodsFor(moduleId: String): Collection<ZincInvokeable> {
            return methodsPerModuleRegistry.getOrDefault(moduleId, mutableMapOf()).values
        }
    }
}

/**
 * Return a list of additional methods that are registered via [registerMethod].
 */
internal fun BflModule.getRegisteredMethods(): Collection<ZincInvokeable> = getRegisteredMethodsFor(id)

/**
 * Return a name for a Constant holding the number of bits in the serialized form of this [BflModule].
 */
fun BflModule.getLengthConstant(): String = "${typeName().camelToSnakeCase().toUpperCase(Locale.getDefault())}_LENGTH"

/**
 * The type definition for the serialized form of this [BflModule].
 */
fun BflModule.getSerializedTypeDef(): ZincTypeDef = zincTypeDef {
    name = "Serialized${typeName()}"
    type = zincArray {
        elementType = ZincPrimitive.Bool
        size = getLengthConstant()
    }
}

fun BflModule.getSerializedBflTypeDef() = BflTypeDef("Serialized${typeName()}", BflArray(bitSize, BflPrimitive.Bool))

fun BflModule.mod(): ZincMod = zincMod { module = getModuleName() }
fun BflModule.use(): ZincUse = zincUse { path = "${getModuleName()}::${typeName()}" }
fun BflModule.useLengthConstant(): ZincUse = zincUse { path = "${getModuleName()}::${getLengthConstant()}" }
fun BflModule.useSerialized(): ZincUse = zincUse { path = "${getModuleName()}::${getSerializedBflTypeDef().typeName()}" }
