
import com.ing.zknotary.gradle.util.Copy
import com.ing.zknotary.gradle.util.Merkle
import com.ing.zknotary.gradle.util.Templates
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
        val template = Templates(circuitName, mergedCircuitOutput, circuitSourcesBase)

        template.templateContents = platformTemplates.resolve("floating_point.zn").readText()
        template.generateFloatingPointsCode(bigDecimalSizes)

        template.templateContents = platformTemplates.resolve("merkle_template.zn").readText()
        template.generateMerkleUtilsCode()

        template.templateContents = platformTemplates.resolve("main_template.zn").readText()
        template.generateMainCode()

        // set correct merkle tree function
        val merkle = Merkle(circuitName, mergedCircuitOutput, circuitSourcesBase)
        merkle.setCorrespondingMerkleTreeFunctionForComponentGroups()
        merkle.setCorrespondingMerkleTreeFunctionForMainTree()

        // remove debug statements
        removeDebugCode(circuitName, mergedCircuitOutput)

        // Generate floating points code for test circuits
        val testTemplate = Templates("test", testSources, circuitSourcesBase)
        testTemplate.templateContents = platformTemplates.resolve("floating_point.zn").readText()
        testTemplate.generateFloatingPointsCode(bigDecimalSizes)
    }
}
