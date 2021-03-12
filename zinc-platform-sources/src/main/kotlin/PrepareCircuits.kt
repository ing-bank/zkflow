import com.ing.zknotary.gradle.util.generateFloatingPointsCode
import com.ing.zknotary.gradle.util.generateMainCode
import com.ing.zknotary.gradle.util.generateMerkleUtilsCode
import com.ing.zknotary.gradle.util.removeDebugCode
import com.ing.zknotary.gradle.util.setCorrespondingMerkleTreeFunctionForComponentGroups
import com.ing.zknotary.gradle.util.setCorrespondingMerkleTreeFunctionForMainTree
import java.io.File

val bigDecimalSizes = setOf(Pair(24, 6), Pair(100, 20))

fun main(args: Array<String>) {

    val root = args[0]
    val zincPlatformTemplatesPath = "$root/src/main/resources/zinc-platform-templates"

    val circuitSourcesBasePath = "$root/circuits"
    val mergedCircuitOutputPath = "$root/build/circuits"

    val circuitSourcesBase = File(circuitSourcesBasePath)

    val circuits = circuitSourcesBase.listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }

    val floatingPointTemplateContents = File("$zincPlatformTemplatesPath/floating_point.zn").readText()
    val merkleTemplateContents = File("$zincPlatformTemplatesPath/merkle_template.zn").readText()
    val mainTemplateContents = File("$zincPlatformTemplatesPath/main_template.zn").readText()

    val testSourcesPath = "$root/src/main/resources/zinc-platform-test-sources"

    circuits?.forEach { circuitName ->
        val targetFilePath = "$mergedCircuitOutputPath/$circuitName/src"
        val constsContent = File("$circuitSourcesBasePath/$circuitName/consts.zn").readText()

        // Generate code from templates
        generateFloatingPointsCode(targetFilePath, floatingPointTemplateContents, bigDecimalSizes)
        generateMerkleUtilsCode(targetFilePath, merkleTemplateContents, constsContent)
        generateMainCode(targetFilePath, mainTemplateContents, constsContent, circuitName)

        // set correct merkle tree function
        setCorrespondingMerkleTreeFunctionForComponentGroups(targetFilePath, constsContent)
        setCorrespondingMerkleTreeFunctionForMainTree(targetFilePath, constsContent)

        // remove debug statements
        removeDebugCode(targetFilePath)

        // Generate floating points code for test circuits
        generateFloatingPointsCode(testSourcesPath, floatingPointTemplateContents, bigDecimalSizes)
    }
}
