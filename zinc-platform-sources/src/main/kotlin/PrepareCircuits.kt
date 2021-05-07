
import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.template.AmountTemplateParameters
import com.ing.zknotary.gradle.template.BigDecimalTemplateParameters
import com.ing.zknotary.gradle.template.CurrencyTemplateParameters
import com.ing.zknotary.gradle.template.LinearPointerTemplateParameters
import com.ing.zknotary.gradle.template.PartyTemplateParameters
import com.ing.zknotary.gradle.template.SecureHashTemplateParameters
import com.ing.zknotary.gradle.template.StringTemplateParameters
import com.ing.zknotary.gradle.template.TemplateParameters
import com.ing.zknotary.gradle.template.TemplateRenderer
import com.ing.zknotary.gradle.template.UniqueIdentifierTemplateParameters
import com.ing.zknotary.gradle.template.X500PrincipalTemplateParameters
import com.ing.zknotary.gradle.util.CodeGenerator
import com.ing.zknotary.gradle.util.MerkleReplacer
import com.ing.zknotary.gradle.util.ZincSourcesCopier
import com.ing.zknotary.gradle.util.removeDebugCode
import java.io.File

val bigDecimalConfigurations = listOf(
    BigDecimalTemplateParameters(24, 6),
    BigDecimalTemplateParameters(100, 20),
    ZKNotaryExtension.floatTemplateParameters,
    ZKNotaryExtension.doubleTemplateParameters
)
val amountConfigurations = bigDecimalConfigurations.map {
    AmountTemplateParameters(it, 8)
}

val stringConfigurations: List<StringTemplateParameters> = listOf(StringTemplateParameters(32))

fun resolveAllTemplateParameters(): List<TemplateParameters> {
    return (
        bigDecimalConfigurations +
            amountConfigurations +
            stringConfigurations +
            listOf(
                UniqueIdentifierTemplateParameters,
                LinearPointerTemplateParameters,
                X500PrincipalTemplateParameters,
                CurrencyTemplateParameters,
                SecureHashTemplateParameters,
            ) + PartyTemplateParameters.all
        )
        .flatMap { it.resolveAllConfigurations() }
        .distinct()
}

fun main(args: Array<String>) {

    val root = args[0]
    val projectVersion = args[1]

    val circuitSourcesBase = File("$root/circuits")
    val mergedCircuitOutput = File("$root/build/circuits")

    val circuits = circuitSourcesBase.listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }
    circuits?.forEach { circuitName ->
        val outputPath = mergedCircuitOutput.resolve(circuitName).resolve("src")

        val circuitSourcesPath = circuitSourcesBase.resolve(circuitName)

        // Copy Zinc sources
        val copier = ZincSourcesCopier(outputPath)
        copier.copyZincCircuitSources(circuitSourcesPath, circuitName, projectVersion)
        copier.copyZincPlatformSources(getPlatformSources(root))

        val consts = circuitSourcesBase.resolve(circuitName).resolve("consts.zn").readText()

        // Render templates
        val templateRenderer = TemplateRenderer(outputPath.toPath()) {
            getTemplateContents(root, it.templateFile)!!
        }
        resolveAllTemplateParameters().forEach {
            templateRenderer.renderTemplate(it)
        }
        // Generate code
        val codeGenerator = CodeGenerator(outputPath)
        getTemplateContents(root, "merkle_template.zn")?.let { codeGenerator.generateMerkleUtilsCode(it, consts) }
        getTemplateContents(root, "main_template.zn")?.let { codeGenerator.generateMainCode(it, consts) }

        // Replace placeholders in Merkle tree functions
        val replacer = MerkleReplacer(outputPath)
        replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
        replacer.setCorrespondingMerkleTreeFunctionForMainTree(consts)

        // remove debug statements
        removeDebugCode(circuitName, mergedCircuitOutput)
    }

    // Render templates for test circuits
    val testTemplateRenderer = TemplateRenderer(getPlatformSourcesPath(root, "zinc-platform-test-sources").toPath()) {
        getTemplateContents(root, it.templateFile)!!
    }
    resolveAllTemplateParameters().forEach {
        testTemplateRenderer.renderTemplate(it)
    }
}

private fun getPlatformSources(root: String): Array<File>? {
    return File("$root/src/main/resources/zinc-platform-sources").listFiles()
}

private fun getPlatformSourcesPath(root: String, sourceName: String): File {
    return File("$root/src/main/resources/$sourceName")
}

private fun getTemplateContents(root: String, templateName: String): String? {
    return File("$root/src/main/resources/zinc-platform-templates").listFiles()?.single {
        it.name.contains(templateName)
    }?.readText()
}
