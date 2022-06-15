package com.ing.zkflow.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.ing.zkflow.util.appendText
import net.corda.core.internal.packageName
import kotlin.reflect.KClass

class MetaInfServiceRegister(private val codeGenerator: CodeGenerator) {
    private val services = mutableMapOf<KClass<*>, List<String>>()
    private var dirty = false

    fun addImplementation(interfaceClass: KClass<*>, vararg implementationClasses: String) {
        services.merge(interfaceClass, implementationClasses.toList()) { list, addition ->
            list + addition
        }
        dirty = true
    }

    fun emit() {
        if (dirty) {
            services.entries.forEach { (kClass, implementations) ->
                codeGenerator.createNewFile(
                    dependencies = Dependencies.ALL_FILES,
                    packageName = "META-INF/services",
                    fileName = kClass.packageName,
                    extensionName = kClass.simpleName!!
                ).appendText(
                    implementations.joinToString("\n") { it }
                )
            }
        }

        dirty = false
    }
}
