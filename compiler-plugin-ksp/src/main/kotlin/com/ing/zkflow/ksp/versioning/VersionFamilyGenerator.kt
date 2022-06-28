package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zkflow.common.versioning.VersionFamily
import com.ing.zkflow.common.versioning.VersionFamilyProvider
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import net.corda.core.internal.packageName
import kotlin.reflect.KClass

/**
 * Generates instances of [VersionFamilyProvider] for sorted states.
 * The states should be sorted in ascending mode, so that the oldest state is at index 0.
 */
class VersionFamilyGenerator(private val codeGenerator: CodeGenerator) {
    private val versionFamilyProviderKClass: KClass<VersionFamilyProvider> = VersionFamilyProvider::class

    /**
     * @param sortedFamilies Map of sorted families according to [StateVersionSorting.sortByConstructors].
     */
    fun generateFamilies(sortedFamilies: Map<KSClassDeclaration, List<KSClassDeclaration>>): ServiceLoaderRegistration {
        val familyProviders = sortedFamilies.entries
            .map { (familyName, members) ->
                generateFamily(familyName.toClassName(), members)
            }

        return ServiceLoaderRegistration(versionFamilyProviderKClass, familyProviders)
    }

    private fun generateFamily(familyClassName: ClassName, sortedMembers: List<KSClassDeclaration>): String {
        val memberClassNames = sortedMembers.map(KSClassDeclaration::toClassName)
        val familyProviderClassName = familyClassName.simpleNames
            .joinToString(separator = "", postfix = "FamilyProvider") { it }
        FileSpec.builder(familyClassName.packageName, familyProviderClassName)
            .addImport(VersionFamily::class.packageName, "${VersionFamily::class.simpleName}")
            .addType(
                TypeSpec.classBuilder(familyProviderClassName)
                    .addSuperinterface(versionFamilyProviderKClass)
                    .addFunction(
                        FunSpec.builder("getFamily")
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode(
                                CodeBlock.of(
                                    """
                                        return VersionFamily(
                                            %1L::class,
                                            listOf(
                                                %2L
                                            )
                                        )
                                    """.trimIndent(),
                                    familyClassName.canonicalName,
                                    memberClassNames.joinToString(", ") { "${it.canonicalName}::class" }
                                )
                            )
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies.ALL_FILES,
            )
        return "${familyClassName.packageName}.$familyProviderClassName"
    }
}
