
import com.ing.zknotary.gradle.util.Copy
import com.ing.zknotary.gradle.util.Merkle
import com.ing.zknotary.gradle.util.TemplateRenderer
import com.ing.zknotary.gradle.util.removeDebugCode
import java.io.File

val bigDecimalSizes = setOf(Pair(24, 6), Pair(100, 20))

fun main(args: Array<String>) {

    val root = args[0]
    val projectVersion = args[1]

    val circuitSourcesBase = File("$root/circuits")
    val mergedCircuitOutput = File("$root/build/circuits")

    val platformTemplates = File("$root/src/main/resources/zinc-platform-templates")
    val platformSources = File("$root/src/main/resources/zinc-platform-sources").listFiles()
    val testSources = File("$root/src/main/resources/zinc-platform-test-sources")

    val circuits = circuitSourcesBase.listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }
    circuits?.forEach { circuitName ->
        // Copy zinc sources
        val copy = Copy(circuitName, mergedCircuitOutput, circuitSourcesBase)
        copy.createCopyZincCircuitSources(projectVersion)
        copy.createCopyZincPlatformSources(platformSources)

        // Generate code from templates
        // TODO: this is a direct code duplication of generateZincPlatformCodeFromTemplates.
        // That logic should be extracted and called from the task and here
        val renderer = TemplateRenderer(mergedCircuitOutput.resolve(circuitName).resolve("src"))

        renderer.generateFloatingPointsCode(
            platformTemplates.resolve("floating_point.zn").readText(),
            bigDecimalSizes
        )

        val consts = circuitSourcesBase.resolve(circuitName).resolve("consts.zn").readText()

        renderer.generateMerkleUtilsCode(platformTemplates.resolve("merkle_template.zn").readText(), consts)
        renderer.generateMainCode(platformTemplates.resolve("main_template.zn").readText(), consts)

        // set correct merkle tree function
        // TODO: This should call renderer.generateMerkleUtilsCode()?
        val merkle = Merkle(circuitName, mergedCircuitOutput, circuitSourcesBase)
        merkle.setCorrespondingMerkleTreeFunctionForComponentGroups()
        merkle.setCorrespondingMerkleTreeFunctionForMainTree()

        // remove debug statements
        removeDebugCode(circuitName, mergedCircuitOutput)

        // Generate floating points code for test circuits
        val testTemplate = TemplateRenderer(testSources)

        testTemplate.generateFloatingPointsCode(
            platformTemplates.resolve("floating_point.zn").readText(),
            bigDecimalSizes
        )
    }
}
