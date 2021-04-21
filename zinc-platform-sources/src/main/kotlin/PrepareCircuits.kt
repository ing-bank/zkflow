
import com.ing.zknotary.gradle.util.MerkleReplacer
import com.ing.zknotary.gradle.util.TemplateRenderer
import com.ing.zknotary.gradle.util.ZincSourcesCopier
import com.ing.zknotary.gradle.util.removeDebugCode
import java.io.File

val bigDecimalSizes = setOf(Pair(24, 6), Pair(100, 20))

val stringConfigurations: List<Short> = listOf(32)

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
        val renderer = TemplateRenderer(outputPath)
        getTemplateContents(root, "string.zn")?.let { renderer.generateStringCode(it, stringConfigurations) }
        getTemplateContents(root, "floating_point.zn")?.let { renderer.generateFloatingPointsCode(it, bigDecimalSizes) }
        getTemplateContents(root, "merkle_template.zn")?.let { renderer.generateMerkleUtilsCode(it, consts) }
        getTemplateContents(root, "main_template.zn")?.let { renderer.generateMainCode(it, consts) }

        // Replace placeholders in Merkle tree functions
        val replacer = MerkleReplacer(outputPath)
        replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
        replacer.setCorrespondingMerkleTreeFunctionForMainTree(consts)

        // remove debug statements
        removeDebugCode(circuitName, mergedCircuitOutput)

        // Generate floating points code for test circuits
        val testTemplate = TemplateRenderer(getPlatformSourcesPath(root, "zinc-platform-test-sources"))
        testTemplate.generateStringCode(
            getPlatformSourcesPath(root, "zinc-platform-templates").resolve("string.zn").readText(),
            stringConfigurations
        )
        testTemplate.generateFloatingPointsCode(
            getPlatformSourcesPath(root, "zinc-platform-templates").resolve("floating_point.zn").readText(),
            bigDecimalSizes
        )
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
