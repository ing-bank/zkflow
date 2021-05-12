package com.ing.zknotary.gradle.extension

import com.ing.serialization.bfl.serializers.DoubleSurrogate
import com.ing.serialization.bfl.serializers.FloatSurrogate
import com.ing.zknotary.gradle.zinc.template.AbstractPartyTemplateParameters
import com.ing.zknotary.gradle.zinc.template.AmountTemplateParameters
import com.ing.zknotary.gradle.zinc.template.BigDecimalTemplateParameters
import com.ing.zknotary.gradle.zinc.template.CurrencyTemplateParameters
import com.ing.zknotary.gradle.zinc.template.LinearPointerTemplateParameters
import com.ing.zknotary.gradle.zinc.template.SecureHashTemplateParameters
import com.ing.zknotary.gradle.zinc.template.StringTemplateParameters
import com.ing.zknotary.gradle.zinc.template.TemplateParameters
import com.ing.zknotary.gradle.zinc.template.UniqueIdentifierTemplateParameters
import com.ing.zknotary.gradle.zinc.template.X500PrincipalTemplateParameters
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import java.io.File

open class ZKNotaryExtension(project: Project) {

    companion object {
        const val NAME = "zkp"

        private const val MERGED_CIRCUIT_BUILD_PATH = "zinc"
        private const val CIRCUIT_SOURCES_BASE_PATH = "src/main/zinc"

        val floatTemplateParameters = BigDecimalTemplateParameters(
            FloatSurrogate.FLOAT_INTEGER_SIZE.toShort(),
            FloatSurrogate.FLOAT_FRACTION_SIZE.toShort(),
            "Float"
        )
        val doubleTemplateParameters = BigDecimalTemplateParameters(
            DoubleSurrogate.DOUBLE_INTEGER_SIZE.toShort(),
            DoubleSurrogate.DOUBLE_FRACTION_SIZE.toShort(),
            "Double"
        )
    }

    @Input
    var stringConfigurations: List<StringTemplateParameters> = emptyList()

    @Input
    var bigDecimalConfigurations: List<BigDecimalTemplateParameters> = emptyList()

    @Input
    var amountConfigurations: List<AmountTemplateParameters> = emptyList()

    @Input
    var zincPlatformSourcesVersion: String? = "1.0-SNAPSHOT"

    @Input
    var notaryVersion: String? = "1.0-SNAPSHOT"

    @OutputDirectory
    val mergedCircuitOutputPath: File = project.buildDir.resolve(MERGED_CIRCUIT_BUILD_PATH)

    @InputDirectory
    val circuitSourcesBasePath: File = project.projectDir.resolve(CIRCUIT_SOURCES_BASE_PATH)

    @Input
    val platformSourcesPath = "zinc-platform-sources/**/*.zn"

    @Input
    val platformLibrariesPath = "zinc-platform-libraries/**/*.zn"

    @Input
    val platformTemplatesPath = "zinc-platform-templates/**/*.zn"

    @Input
    val platformSamplesPath = "zinc-platform-samples/**/*.zn"

    @Input
    val merkleTemplate = "merkle_template.zn"

    @Input
    val mainTemplate = "main_template.zn"

    /*
     * Pre-defined collection of configurations to generate zinc sources for
     * standard data types like float and double.
     */
    private val fixedTemplateParameters: List<TemplateParameters> = listOf(
        floatTemplateParameters,
        doubleTemplateParameters,
        UniqueIdentifierTemplateParameters,
        LinearPointerTemplateParameters,
        X500PrincipalTemplateParameters,
        CurrencyTemplateParameters,
        SecureHashTemplateParameters,
    ) + AbstractPartyTemplateParameters.all

    fun resolveAllTemplateParameters(): List<TemplateParameters> {
        return (fixedTemplateParameters + stringConfigurations + bigDecimalConfigurations + amountConfigurations)
            .flatMap { it.resolveAllConfigurations() }
            .distinct()
    }
}
