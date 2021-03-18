
import com.ing.zknotary.gradle.util.Renderer
import com.ing.zknotary.gradle.util.TemplateRenderer
import com.ing.zknotary.gradle.util.removeDebugCode
import java.io.File

val bigDecimalSizes = setOf(Pair(24, 6), Pair(100, 20))

fun main(args: Array<String>) {

    val root = args[0]
    val projectVersion = args[1]

    val circuitSourcesBase = File("$root/circuits")
    val mergedCircuitOutput = File("$root/build/circuits")

    val circuits = circuitSourcesBase.listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }
    circuits?.forEach { circuitName ->

        val renderer = Renderer(mergedCircuitOutput.resolve(circuitName).resolve("src"))
        val consts = circuitSourcesBase.resolve(circuitName).resolve("consts.zn").readText()

        // Copy Zinc sources
        getPlatformSourcesFiles(root, "zinc-platform-sources")?.let { renderer.operateCopyRenderer(it, circuitSourcesBase, circuitName, projectVersion) }

        // Generate code from templates
        getPlatformSourcesFiles(root, "zinc-platform-templates")?.let { renderer.operateTemplateRenderer(it, consts, bigDecimalSizes) }

        // Set correct merkle tree function
        renderer.operateMerkleRenderer(consts)

        // remove debug statements
        removeDebugCode(circuitName, mergedCircuitOutput)

        // Generate floating points code for test circuits
        val testTemplate = TemplateRenderer(getPlatformSourcesPath(root, "zinc-platform-test-sources"))
        testTemplate.generateFloatingPointsCode(
            getPlatformSourcesPath(root, "zinc-platform-templates").resolve("floating_point.zn").readText(),
            bigDecimalSizes
        )
    }
}

private fun getPlatformSourcesFiles(root: String, sourceName: String): Array<File>? {
    return File("$root/src/main/resources/$sourceName").listFiles()
}

private fun getPlatformSourcesPath(root: String, sourceName: String): File {
    return File("$root/src/main/resources/$sourceName")
}
