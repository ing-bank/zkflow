package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zinc.poet.ZincFile
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincTypeDef
import java.util.Locale
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

interface BflModule : BflType {
    fun generateZincFile(codeGenerationOptions: CodeGenerationOptions): ZincFile

    val serializedTypeDef: ZincTypeDef

    fun getModuleName(): String = id.camelToSnakeCase()

    fun getLengthConstant(): String = "${id.camelToSnakeCase().toUpperCase(Locale.getDefault())}_LENGTH"

    /**
     * Return a list of additional methods that are registered via [registerMethod].
     */
    fun getRegisteredMethods(): Collection<ZincFunction> = getRegisteredMethodsFor(id)

    val customMethods: Collection<ZincFunction>

    companion object {
        private val methodsPerModuleRegistry: MutableMap<String, MutableMap<String, ZincFunction>> = mutableMapOf()

        /**
         * Registers a [method] for a module with id [moduleId].
         */
        fun registerMethod(moduleId: String, method: ZincFunction) {
            val methodsRegistry = methodsPerModuleRegistry.getOrPut(moduleId) { mutableMapOf() }
            if (methodsRegistry[method.getName()] != null) {
                throw IllegalStateException("A method with name ${method.getName()} is already registered for $moduleId.")
            }
            methodsRegistry[method.getName()] = method
        }

        private fun getRegisteredMethodsFor(moduleId: String): Collection<ZincFunction> {
            return methodsPerModuleRegistry.getOrDefault(moduleId, mutableMapOf()).values
        }
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
    val methodsFromAnnotations = getAllZincMethodGenerators()
        .flatMap {
            when (val result = it.call(this, codeGenerationOptions)) {
                is ZincFunction -> listOf(result)
                is List<*> -> result as? List<ZincFunction>
                    ?: throw IllegalArgumentException("Unsupported zinc function list type: ${result::class.qualifiedName} on ${this::class.qualifiedName}::${it.name}()")
                null -> throw IllegalArgumentException("Unsupported zinc function type: null on ${this::class.qualifiedName}::${it.name}()")
                else -> throw IllegalArgumentException("Unsupported zinc function type: ${result::class.qualifiedName} on ${this::class.qualifiedName}::${it.name}()")
            }
        }
    return methodsFromAnnotations + getRegisteredMethods()
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
        .filter { it.hasAnnotation<ZincMethod>() || it.hasAnnotation<ZincMethodList>() }
        .map {
            if (it.hasAnnotation<ZincMethod>()) {
                Pair(it, it.findAnnotation<ZincMethod>()!!.order)
            } else {
                Pair(it, it.findAnnotation<ZincMethodList>()!!.order)
            }
        }
}
