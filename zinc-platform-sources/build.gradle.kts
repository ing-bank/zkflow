plugins {
    java
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("zkGenerator") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ingzkp/zk-notary")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// Global consts
val root = "${project.rootDir.absolutePath}/zinc-platform-sources"
val resources = "$root/src/main/resources"

val zincPlatformSourcesPath = "$resources/zinc-platform-sources"
val zincPlatformTemplatesPath = "$resources/zinc-platform-templates"

val circuitSourcesBasePath = "$root/circuits"
val mergedCircuitOutputPath = "$root/build/circuits"

val zincPlatformSources = File(zincPlatformSourcesPath)
val circuitSourcesBase = File(circuitSourcesBasePath)
val mergedCircuitOutput = File(mergedCircuitOutputPath)

val bigDecimalSizes = setOf(Pair(24, 6), Pair(100, 20))

val circuits = circuitSourcesBase.listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }

task("copyZincSources") {
    circuits?.forEach { circuitName ->
        // Copy circuit sources
        copy {
            from(circuitSourcesBase.resolve(circuitName))
            into(mergedCircuitOutput.resolve(circuitName).resolve("src"))
        }
        // Copy platform sources
        copy {
            from(zincPlatformSources)
            into(mergedCircuitOutput.resolve(circuitName).resolve("src"))
        }
    }
}

task("generateFloatingPointFromTemplate") {
    val templateContents = File("$zincPlatformTemplatesPath/floating_point.zn").readText()
    circuits?.forEach { circuitName ->
        bigDecimalSizes.forEach {
            val floatingPointContent = templateContents.replace("\${INTEGER_SIZE_PLACEHOLDER}", it.first.toString())
                .replace("\${FRACTION_SIZE_PLACEHOLDER}", it.second.toString())
            val sizeSuffix = "${it.first}_${it.second}"
            val targetFile = mergedCircuitOutput.resolve(circuitName).resolve("src/floating_point_$sizeSuffix.zn")
            targetFile.delete()
            targetFile.createNewFile()
            targetFile.writeBytes(floatingPointContent.toByteArray())
        }
    }
}

task("generateMerkleUtils") {
    val templateContents = File("$zincPlatformTemplatesPath/merkle_template.zn").readText()
    circuits?.forEach { circuitName ->
        val targetFile = mergedCircuitOutput.resolve(circuitName).resolve("src/merkle_utils.zn")
        targetFile.delete()
        targetFile.createNewFile()
        targetFile.writeText("//! Limited-depth recursion for Merkle tree construction\n")
        targetFile.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zinc-platform-sources/build.gradle.kts\n")
        targetFile.appendText(
            """
mod platform_component_group_leaf_digest_dto;
mod platform_crypto_utils;
mod platform_node_digest_dto;   
 
use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestBits;
use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestDto;
use platform_component_group_leaf_digest_dto::COMPONENT_GROUP_LEAF_DIGEST_BITS;
use platform_crypto_utils::pedersen_to_padded_bits;
use platform_crypto_utils::concatenate_component_group_leaf_digests;
use platform_crypto_utils::concatenate_node_digests;
use platform_node_digest_dto::NodeDigestBits;
use platform_node_digest_dto::NodeDigestDto;
use platform_node_digest_dto::NODE_DIGEST_BITS;
use std::crypto::pedersen;

            """
        )
        val constsTemplate = File("${circuitSourcesBase.resolve(circuitName)}/consts.zn").readText()
        val search = "GROUP_SIZE: u16 = (\\d+);".toRegex()
        var total = 3 // notary, timewindow, and parameters group size
        search.findAll(constsTemplate).forEach {
            total += it.groupValues[1].toInt()
        }

        fun isPow2(num: Int) = num and (num - 1) == 0
        val fullMerkleLeaves = run {
            var l = total
            while (!isPow2(l)) {
                l++
            }
            l
        }

        targetFile.appendText(
            getMerkleTree(
                templateContents, fullMerkleLeaves, digestSnakeCase = "node_digests",
                digestCamelCase = "NodeDigest",
                digestBits = "NODE_DIGEST_BITS"
            )
        )

        targetFile.appendText(
            getMerkleTree(
                templateContents, fullMerkleLeaves, digestSnakeCase = "component_group_leaf_digests",
                digestCamelCase = "ComponentGroupLeafDigest",
                digestBits = "COMPONENT_GROUP_LEAF_DIGEST_BITS"
            )
        )
    }
}

task("generateMain") {
    val templateContents = File("$zincPlatformTemplatesPath/main_template.zn").readText()
    circuits?.forEach { circuitName ->
        val targetFile = mergedCircuitOutput.resolve(circuitName).resolve("src/main.zn")
        targetFile.delete()
        targetFile.createNewFile()
        targetFile.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zinc-platform-sources/build.gradle.kts\n")
        targetFile.appendText("//! The '$circuitName' main module.")
        targetFile.appendText(
            """
mod consts;
mod contract_rules;
mod platform_component_group_leaf_digest_dto;
mod platform_merkle_tree;
mod platform_node_digest_dto;
mod platform_utxo_digests;
mod platform_zk_prover_transaction;
 
use consts::INPUT_GROUP_SIZE;
use consts::REFERENCE_GROUP_SIZE; 
use contract_rules::check_contract_rules;
use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestDto;
use platform_component_group_leaf_digest_dto::COMPONENT_GROUP_LEAF_DIGEST_BYTES;
use platform_merkle_tree::build_merkle_tree;
use platform_node_digest_dto::NodeDigestDto;
use platform_node_digest_dto::NODE_DIGEST_BYTES;
use platform_utxo_digests::compute_input_utxo_digests;
use platform_utxo_digests::compute_reference_utxo_digests;
use platform_zk_prover_transaction::Witness;
"""
        )

        val constsTemplate = File("${circuitSourcesBase.resolve(circuitName)}/consts.zn").readText()
        val inputHashes = replaceComponentPlaceholders(constsTemplate, "input")
        val referenceHashes = replaceComponentPlaceholders(constsTemplate, "reference")

        val mainContent = templateContents.replace("\${COMMAND_NAME_PLACEHOLDER}", circuitName)
            .replace("\${INPUT_HASH_PLACEHOLDER}", inputHashes)
            .replace("\${REFERENCE_HASH_PLACEHOLDER}", referenceHashes)
        targetFile.appendBytes(mainContent.toByteArray())
    }
}

task("generateMerkleFunctionCalls") {
    circuits?.forEach { circuitName ->
        // Update component subtrees
        val componentGroupFiles = File("${mergedCircuitOutput.resolve(circuitName)}/").walk().filter {
            it.name.contains("platform_components")
        }
        componentGroupFiles.forEach { file ->
            // Filter component group files
            val fileContent = file.readText()
            // Find which component group
            val componentRegex = "struct (\\w+)(ComponentGroup)".toRegex()
            var componentGroupName = componentRegex.find(fileContent)?.groupValues?.get(1)
            if (componentGroupName?.endsWith("s")!!) componentGroupName = componentGroupName.dropLast(1)

            val constsTemplate = File("${circuitSourcesBase.resolve(circuitName)}/consts.zn").readText()
            val constsRegex = "${componentGroupName.toUpperCase()}_GROUP_SIZE: u16 = (\\d+);".toRegex()
            val componentGroupSize = constsRegex.find(constsTemplate)?.groupValues?.get(1)?.toInt()

            // Find the size of the component group in consts.zn
            file.delete()
            file.createNewFile()
            val updatedFileContent = callAppropriateMerkleTreeFunction(componentGroupSize, fileContent)

            file.appendBytes(updatedFileContent.toByteArray())
        }

        // Update main merkle tree
        val platformMerkleTree = File("${mergedCircuitOutput.resolve(circuitName)}/").walk().find { it.name.contains("platform_merkle_tree.zn") }
        if (platformMerkleTree != null) {
            val fileContent = platformMerkleTree.readText()
            platformMerkleTree.delete()
            platformMerkleTree.createNewFile()

            val constsTemplate = File("${circuitSourcesBase.resolve(circuitName)}/consts.zn").readText()
            val search = "GROUP_SIZE: u16 = (\\d+);".toRegex()
            var total = 3 // notary, timewindow, and parameters group size
            search.findAll(constsTemplate).forEach {
                total += it.groupValues[1].toInt()
            }

            fun isPow2(num: Int) = num and (num - 1) == 0
            val fullMerkleLeaves = run {
                var l = total
                while (!isPow2(l)) {
                    l++
                }
                l
            }
            val updatedFileContent = fileContent.replace("\${GROUP_SIZE_PLACEHOLDER}", fullMerkleLeaves.toString())
            platformMerkleTree.appendBytes(updatedFileContent.toByteArray())
        }
    }
}

task("removeDebugStatements") {
    circuits?.forEach { circuitName ->
        File("${mergedCircuitOutput.resolve(circuitName)}/").walk().forEach { file ->
            // Skip directories
            if (file.name.contains(".zn")) {
                val lines = file.readLines()
                file.delete()
                file.createNewFile()

                lines.filter { s: String -> !s.contains("dbg!") }.map {
                    file.appendText("$it\n")
                }
            }
        }
    }
}

task("buildCircuits") {
    mustRunAfter("rustfmtCheck")
    dependsOn("copyZincSources",
        "generateFloatingPointFromTemplate",
        "generateMerkleUtils",
        "generateMain",
        "generateMerkleFunctionCalls",
        "removeDebugStatements")

    doLast {
        circuits?.forEach { circuitName ->
            val circuitPath = File("$mergedCircuitOutputPath/$circuitName")
            // Create Zargo.toml
            val zargoFile = File("${circuitPath.absolutePath}/Zargo.toml")
            zargoFile.writeText(
"""
    [circuit]
    name = "$circuitName"
    version = "${project.version}"                                
"""
            )

            // Compile circuit
            exec {
                workingDir = circuitPath
                executable = "zargo"
                args = listOf("clean", "-v")
            }
        }
    }
}

task("rustfmt") {
    circuits?.forEach {
        outputs.dir("${mergedCircuitOutput.resolve(it)}/")
        outputs.files.forEach { file ->
            if (file.name.contains(".zn")) {
                exec {
                    commandLine("rustfmt", file)
                }
            }
        }
    }
}

task("rustfmtCheck") {
    mustRunAfter("generateMerkleUtils")
    circuits?.forEach {
        outputs.dir("${mergedCircuitOutput.resolve(it)}/")
        outputs.files.forEach { file ->
            if (file.name.contains(".zn")) {
                exec {
                    commandLine("rustfmt", "--check", it)
                }
            }
        }
    }
}

task("circuits") {
    dependsOn("rustfmtCheck", "buildCircuits")
}

// Auxiliary functions
fun getMerkleTree(templateContents: String, fullMerkleLeaves: Int, digestSnakeCase: String, digestCamelCase: String, digestBits: String): String {
    var digestMerkleFunctions = ""
    // Compute the root
    digestMerkleFunctions +=
        """
fn get_merkle_tree_from_2_$digestSnakeCase(leaves: [${digestCamelCase}Bits; 2]) -> ${digestCamelCase}Bits {
    pedersen_to_padded_bits(pedersen(concatenate_$digestSnakeCase(leaves[0], leaves[1])).0)
}
"""
    if (fullMerkleLeaves > 2) {
        var leaves = 4
        do {
            val levelUp = leaves / 2
            digestMerkleFunctions += templateContents.replace("\${NUM_LEAVES_PLACEHOLDER}", leaves.toString())
                .replace("\${DIGEST_TYPE_PLACEHOLDER}", digestSnakeCase)
                .replace("\${DIGEST_BITS_TYPE_PLACEHOLDER}", "${digestCamelCase}Bits")
                .replace("\${DIGEST_BITS_PLACEHOLDER}", digestBits)
                .replace("\${DTO_PLACEHOLDER}", "${digestCamelCase}Dto")
                .replace("\${LEVEL_UP_PLACEHOLDER}", levelUp.toString())
            leaves *= 2
        } while (leaves <= fullMerkleLeaves)
    }
    return digestMerkleFunctions
}

fun replaceComponentPlaceholders(template: String, componentGroup: String): String {
    val componentRegex = "${componentGroup.toUpperCase()}_GROUP_SIZE: u16 = (\\d+);".toRegex()
    val componentGroupSize = componentRegex.find(template)?.groupValues?.get(1)?.toInt()

    if (componentGroupSize != null) {
        return when {
            componentGroupSize > 0 -> {
                """compute_${componentGroup}_utxo_digests( 
            witness.transaction.${componentGroup}s.components,
            witness.${componentGroup}_nonces,
        )"""
            }
            componentGroupSize == 0 -> {
                """[ComponentGroupLeafDigestDto {
            bytes: [0; COMPONENT_GROUP_LEAF_DIGEST_BYTES],
        }; ${componentGroup.toUpperCase()}_GROUP_SIZE]"""
            }
            else -> {
                throw IllegalArgumentException("Negative values are not allowed for ${componentGroup.toUpperCase()}_GROUP_SIZE in consts.zn")
            }
        }
    } else {
        throw IllegalArgumentException("Unknown value for ${componentGroup.toUpperCase()}_GROUP_SIZE in consts.zn")
    }
}

fun callAppropriateMerkleTreeFunction(componentGroupSize: Int?, fileContent: String): String {
    return if (componentGroupSize != null) {
        when {
            // This condition is executed when there is no element in the component group.
            // The return value is allOnesHash
            componentGroupSize == 0 -> {
                fileContent.replace(
                    "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
                    """
        // Return all ones hash
        [true; NODE_DIGEST_BITS]
        """
                ).replace("\${GROUP_SIZE_PLACEHOLDER}", "2")
            }
            // This condition is executed when the defined group size is an exact power of 2.
            // The return value is the merkle tree function that corresponds to the group size.
            componentGroupSize % 2 == 0 -> {
                fileContent.replace(
                    "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
                    """
        let component_leaf_hashes = compute_leaf_hashes(this, privacy_salt);

        get_merkle_tree_from_${componentGroupSize}_component_group_leaf_digests(component_leaf_hashes)
        """
                ).replace("\${GROUP_SIZE_PLACEHOLDER}", componentGroupSize.toString())
            }
            // This condition is executed when the defined group size is not a power of 2.
            // The function finds the next power of 2 and adds padded values to the group.
            // The return value is the merkle tree function that corresponds to the padded group size.
            else -> {
                val paddedGroupSize = getNextPowerOfTwo(componentGroupSize)
                fileContent.replace(
                    "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
                    """
        let component_leaf_hashes = compute_leaf_hashes(this, privacy_salt);

        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; $paddedGroupSize];
        for i in 0..$componentGroupSize {
            padded_leaves[i] = component_leaf_hashes[i];
        }

        get_merkle_tree_from_${paddedGroupSize}_component_group_leaf_digests(padded_leaves)
        """
                ).replace("\${GROUP_SIZE_PLACEHOLDER}", paddedGroupSize.toString())
            }
        }
    } else {
        // This condition is executed when there is no component group size defined.
        // It is possible for notary, timeWindow, parameters groups
        // In that case, we call Merkle tree function for 2 with padded leaves
        fileContent.replace(
            "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
            """
        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; 2];
        padded_leaves[0] = component_leaf_hash;

        get_merkle_tree_from_2_component_group_leaf_digests(padded_leaves)
        """
        ).replace("\${GROUP_SIZE_PLACEHOLDER}", "2")
    }
}

fun getNextPowerOfTwo(value: Int): Int {
    val highestOneBit = Integer.highestOneBit(value)
    return if (value == 1) {
        2
    } else {
        highestOneBit shl 1
    }
}
