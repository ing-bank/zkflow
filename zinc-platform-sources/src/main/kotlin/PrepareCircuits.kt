
import com.ing.zknotary.gradle.extension.AmountTemplateParameters
import com.ing.zknotary.gradle.extension.BigDecimalTemplateParameters
import com.ing.zknotary.gradle.extension.StringTemplateParameters
import com.ing.zknotary.gradle.extension.TemplateParameters
import com.ing.zknotary.gradle.extension.UniqueIdentifierTemplateParameters
import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.util.MerkleReplacer
import com.ing.zknotary.gradle.util.TemplateRenderer
import com.ing.zknotary.gradle.util.ZincSourcesCopier
import com.ing.zknotary.gradle.util.removeDebugCode
import java.io.File

val bigDecimalConfigurations = listOf(
    BigDecimalTemplateParameters(24, 6),
    BigDecimalTemplateParameters(100, 20),
    ZKNotaryExtension.float,
    ZKNotaryExtension.double
)
val amountConfigurations = bigDecimalConfigurations.map {
    AmountTemplateParameters(it, 8)
}

val stringConfigurations: List<StringTemplateParameters> = listOf(StringTemplateParameters(32))

fun resolveAllTemplateParameters(): List<TemplateParameters> {
    return (bigDecimalConfigurations + amountConfigurations + stringConfigurations + UniqueIdentifierTemplateParameters)
        .flatMap { it.resolveAllConfigurations() }.distinct()
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

        // Generate code from templates
        val renderer = TemplateRenderer(outputPath) {
            getTemplateContents(root, it)!!
        }
        resolveAllTemplateParameters().forEach {
            renderer.generateTemplate(it)
        }
        getTemplateContents(root, "merkle_template.zn")?.let { renderer.generateMerkleUtilsCode(it, consts) }
        getTemplateContents(root, "main_template.zn")?.let { renderer.generateMainCode(it, consts) }

        // Replace placeholders in Merkle tree functions
        val replacer = MerkleReplacer(outputPath)
        replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
        replacer.setCorrespondingMerkleTreeFunctionForMainTree(consts)

        // remove debug statements
        removeDebugCode(circuitName, mergedCircuitOutput)
    }

    // Generate floating points code for test circuits
    val testTemplate = TemplateRenderer(getPlatformSourcesPath(root, "zinc-platform-test-sources")) {
        getTemplateContents(root, it)!!
    }
    resolveAllTemplateParameters().forEach {
        testTemplate.generateTemplate(it)
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
