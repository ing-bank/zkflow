package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflModule.Companion.getRegisteredMethodsFor
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincTypeDef
import com.ing.zkflow.util.requireInstanceOf
import com.ing.zkflow.util.requireNotNull
import java.util.Locale
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

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

/**
 * This annotation marks a method as generating a zinc method.
 *
 * The method should not accept any parameters, and return a [String] or [ZincFunction] or [ZincMethod] with the zinc function implementation.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ZincMethod(
    val order: Int
)

/**
 * This annotation marks a method as generating a list of zinc methods.
 *
 * The method should not accept any parameters, and return a [List<ZincFunction>] with the zinc function implementations.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ZincMethodList(
    val order: Int
)

internal fun <T : BflModule> T.getAllMethods(codeGenerationOptions: CodeGenerationOptions): Sequence<ZincFunction> {
    val methodsFromAnnotations: Sequence<ZincFunction> = getAllZincMethodGenerators()
        .flatMap { method ->
            when (val result = invokeZincMethodGenerator(method, codeGenerationOptions)) {
                null -> throw IllegalArgumentException("Unsupported zinc function type: null on ${this::class.qualifiedName}::${method.name}()")
                is List<*> -> result.map {
                    it.requireNotNull {
                        "Unsupported zinc function type: null on ${this::class.qualifiedName}::${method.name}()"
                    }.requireInstanceOf {
                        "Unsupported zinc function type: ${result::class.qualifiedName} on ${this::class.qualifiedName}::${method.name}()"
                    }
                }
                else -> listOf(
                    result.requireInstanceOf {
                        "Unsupported zinc function type: ${result::class.qualifiedName} on ${this::class.qualifiedName}::${method.name}()"
                    }
                )
            }
        }
    return methodsFromAnnotations + getRegisteredMethods()
}

private fun <T : BflModule> T.invokeZincMethodGenerator(
    method: KFunction<*>,
    codeGenerationOptions: CodeGenerationOptions
) = when (method.parameters.size) {
    1 -> method.call(this)
    2 -> method.call(this, codeGenerationOptions)
    else -> error("Methods annotated with ${ZincMethod::class.simpleName} or ${ZincMethodList::class.simpleName} should take either no arguments, or a single ${CodeGenerationOptions::class.simpleName} as argument")
}

private fun <T : BflModule> T.getAllZincMethodGenerators(): Sequence<KFunction<*>> {
    return getOrderedZincMethods()
        .sortedBy { it.second }
        .map { it.first }
}

private fun <T : BflModule> T.getOrderedZincMethods(): Sequence<Pair<KFunction<*>, Int>> {
    return this::class.memberFunctions
        .asSequence()
        .onEach { it.isAccessible = true }
        .flatMap { method ->
            method.findAnnotation<ZincMethod>()?.let {
                sequenceOf(Pair(method, it.order))
            } ?: method.findAnnotation<ZincMethodList>()?.let {
                sequenceOf(Pair(method, it.order))
            } ?: emptySequence()
        }
}
