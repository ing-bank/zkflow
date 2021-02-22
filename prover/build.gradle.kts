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

// The following values must be defined as global constants.
// It is impossible to move them into the CommandCircuit as static components
// because of an incomprehensible reason.
// > Could not open cache directory 6f7l1lbu6cqg0vysvf72z734b (/Users/vic/.gradle/caches/6.5.1/gradle-kotlin-dsl/6f7l1lbu6cqg0vysvf72z734b).
//    > Internal compiler error: Back-end (JVM) Internal error: wrong bytecode generated for static initializer
//      <no bytecode>
val root = "${project.rootDir.absolutePath}/prover"
val sharedModules = "$root/modules/shared"

val create = CommandCircuit.Create(root)
val move = CommandCircuit.Move(root)

val circuits: List<CommandCircuit> = listOf(create, move)
val distinctModules =
    circuits.fold(emptySet<String>()) { acc, commandCircuit -> acc + commandCircuit.orderedModules }
val merkleUtilsPath = "$sharedModules/utils/merkle_utils.zn"

task("buildCircuits") {
    mustRunAfter("rustfmtCheck")
    dependsOn("generateMerkleUtils")

    circuits.forEach {
        outputs.dir(it.circuitRoot)
        inputs.files(it.orderedModules)
    }

    doLast {
        circuits.forEach {
            it.build()

            // Compile circuit
            val circuitPath = File(it.circuitRoot)
            exec {
                workingDir = circuitPath
                executable = "zargo"
                args = listOf("clean", "-v")
            }
        }
    }
}

task("rustfmt") {
    circuits.forEach {
        outputs.dir(it.circuitRoot)
        inputs.files(it.orderedModules)
    }

    doLast {
        distinctModules
            .forEach {
                exec {
                    commandLine("rustfmt", it)
                }
            }
    }
}

task("rustfmtCheck") {
    mustRunAfter("generateMerkleUtils")

    circuits.forEach {
        outputs.dir(it.circuitRoot)
        inputs.files(it.orderedModules)
    }

    doLast {
        distinctModules
            .forEach {
                exec {
                    commandLine("rustfmt", "--check", it)
                }
            }
    }
}

val merkleLeaves = 9
task("generateMerkleUtils") {
    outputs.file(merkleUtilsPath)
    circuits.forEach {
        inputs.files(it.orderedModules.filter { !it.contains("merkle_utils.zn") })
    }

    // #######################################################
    // It is of UTTER importance to not reformat code snippets
    // #######################################################

    doLast {
        fun isPow2(num: Int) = num and (num - 1) == 0

        val fullLeaves = run {
            var l = merkleLeaves
            while (!isPow2(l)) {
                l++
            }
            l
        }

        val merkleUtils = File(merkleUtilsPath)
        merkleUtils.writeText("//! Limited-depth recursion for Merkle tree construction\n")
        merkleUtils.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in prover/build.gradle.kts\n\n")

        merkleUtils.appendText("//! Merkle tree construction for NodeDigestBits")
        merkleUtils.appendText(
            """
fn get_merkle_tree_from_2_node_digests(leaves: [NodeDigestBits; 2]) -> NodeDigestBits {
    dbg!("Consuming 2 leaves");
    dbg!("0: {}", NodeDigestDto::from_bits_to_bytes(leaves[0]));
    dbg!("1: {}", NodeDigestDto::from_bits_to_bytes(leaves[1]));
    pedersen_to_padded_bits(pedersen(concatenate_node_digests(leaves[0], leaves[1])).0)
}
"""
        )

        var leaves = 4

        do {
            val levelUp = leaves / 2

            merkleUtils.appendText(
                """
fn get_merkle_tree_from_${leaves}_node_digests(leaves: [NodeDigestBits; $leaves]) -> NodeDigestBits {
    dbg!("Consuming $leaves leaves");
    let mut new_leaves = [[false; NODE_DIGEST_BITS]; $levelUp];
    for i in 0..$levelUp {
        new_leaves[i] = pedersen_to_padded_bits(
            pedersen(concatenate_node_digests(leaves[2 * i], leaves[2 * i + 1])).0,
        );
        dbg!(
            "{}: {}",                                         // dbg!
            2 * i,                                            // dbg!
            NodeDigestDto::from_bits_to_bytes(leaves[2 * i])  // dbg!
        ); //dbg!
        dbg!(
            "{}: {}",                                             // dbg!
            2 * i + 1,                                            // dbg!
            NodeDigestDto::from_bits_to_bytes(leaves[2 * i + 1])  // dbg!
        ); // dbg!
        dbg!(
            "Digest: {}",                                     // dbg!
            NodeDigestDto::from_bits_to_bytes(new_leaves[i])  // dbg!
        ); // dbg!
    }
    dbg!("");
    get_merkle_tree_from_${levelUp}_node_digests(new_leaves)
}
"""
            )
            leaves *= 2
        } while (leaves <= fullLeaves)

        merkleUtils.appendText(
            "\n/// Merkle tree construction for ComponentGroupLeafDigestBits.\n" +
                "/// Use it only for the computation of a component sub-Merkle tree from component group leaf hashes."
        )
        merkleUtils.appendText(
            """
fn get_merkle_tree_from_2_component_group_leaf_digests(
    leaves: [ComponentGroupLeafDigestBits; 2],
) -> NodeDigestBits {
    dbg!("Consuming 2 leaves");
    dbg!(
        "0: {}",                                                    // dbg!
        ComponentGroupLeafDigestDto::from_bits_to_bytes(leaves[0])  // dbg!
    ); //dbg!
    dbg!(
        "1: {}",                                                    // dbg!
        ComponentGroupLeafDigestDto::from_bits_to_bytes(leaves[1])  // dbg!
    ); // dbg!
    pedersen_to_padded_bits(
        pedersen(concatenate_component_group_leaf_digests(
            leaves[0], leaves[1],
        ))
        .0,
    )
}
"""
        )

        leaves = 4

        do {
            val levelUp = leaves / 2

            merkleUtils.appendText(
                """
fn get_merkle_tree_from_${leaves}_component_group_leaf_digests(
    leaves: [ComponentGroupLeafDigestBits; $leaves],
) -> NodeDigestBits {
    dbg!("Consuming $leaves leaves");
    let mut new_leaves = [[false; NODE_DIGEST_BITS]; $levelUp];
    for i in 0..$levelUp {
        new_leaves[i] = pedersen_to_padded_bits(
            pedersen(concatenate_component_group_leaf_digests(
                leaves[2 * i],
                leaves[2 * i + 1],
            ))
            .0,
        );
        dbg!(
            "{}: {}",                                                       // dbg!
            2 * i,                                                          // dbg!
            ComponentGroupLeafDigestDto::from_bits_to_bytes(leaves[2 * i])  // dbg!
        ); // dbg!
        dbg!(
            "{}: {}",                                                           // dbg!
            2 * i + 1,                                                          // dbg!
            ComponentGroupLeafDigestDto::from_bits_to_bytes(leaves[2 * i + 1])  // dbg!
        ); // dbg!
        dbg!(
            "Digest: {}",                                                   // dbg!
            ComponentGroupLeafDigestDto::from_bits_to_bytes(new_leaves[i])  // dbg!
        ); // dbg!
    }
    dbg!("");
    get_merkle_tree_from_${levelUp}_node_digests(new_leaves)
}
"""
            )
            leaves *= 2
        } while (leaves <= fullLeaves)
    }
}

task("circuits") {
    dependsOn("generateMerkleUtils", "rustfmtCheck", "buildCircuits")
}

sealed class CommandCircuit(command: String, absoluteRoot: String) {
    val modules = "$absoluteRoot/modules"
    val circuits = "$absoluteRoot/circuits"
    val isDbgOn = false

    // Combine modules in a given order,
    abstract val _orderedModules: List<String>
    val orderedModules: Set<String>
        get() = _orderedModules.map { "$modules/$it" }.toSet()

    // write the result to this file.
    val circuitRoot = "$circuits/$command"
    val circuitPath = "$circuitRoot/src/main.zn"

    // a map where each command has its corresponding consts path
    val commandConstsMap = mapOf(
        "create" to "$modules/create/utils/consts.zn",
        "move" to "$modules/move/utils/consts.zn"
    )

    class Create(root: String) : CommandCircuit("create", root) {
        override val _orderedModules = listOf(
            "shared/utils/preamble.zn",
            "shared/utils/consts.zn",
            "create/utils/consts.zn",
            "shared/component_group_enum.zn",
            "shared/crypto/privacy_salt.zn",
            "shared/dto/component_group_leaf_digest_dto.zn",
            "shared/dto/node_digest_dto.zn",
            "shared/dto/nonce_digest_dto.zn",
            "shared/debug/debug_utils.zn",
            "shared/utils/crypto_utils.zn",
            "shared/crypto/pub_key.zn",
            "shared/utils/merkle_utils.zn",
            "shared/state_and_ref.zn",
            "shared/components/inputs.zn",
            "shared/components/outputs.zn",
            "shared/components/references.zn",
            "shared/components/commands.zn",
            "shared/components/attachments.zn",
            "shared/components/notary.zn",
            "shared/components/time_window.zn",
            "shared/components/parameters.zn",
            "shared/components/signers.zn",
            "shared/zk_prover_transaction.zn",
            "shared/utils/utxo_digests.zn",
            "shared/merkle_tree.zn",
            "create/validate/contract_rules.zn",
            "create/main.zn"
        )
    }

    class Move(root: String) : CommandCircuit("move", root) {
        override val _orderedModules = listOf(
            "shared/utils/preamble.zn",
            "shared/utils/consts.zn",
            "move/utils/consts.zn",
            "shared/component_group_enum.zn",
            "shared/crypto/privacy_salt.zn",
            "shared/dto/component_group_leaf_digest_dto.zn",
            "shared/dto/node_digest_dto.zn",
            "shared/dto/nonce_digest_dto.zn",
            "shared/debug/debug_utils.zn",
            "shared/utils/crypto_utils.zn",
            "shared/crypto/pub_key.zn",
            "shared/utils/merkle_utils.zn",
            "shared/state_and_ref.zn",
            "shared/components/inputs.zn",
            "shared/components/outputs.zn",
            "shared/components/references.zn",
            "shared/components/commands.zn",
            "shared/components/attachments.zn",
            "shared/components/notary.zn",
            "shared/components/time_window.zn",
            "shared/components/parameters.zn",
            "shared/components/signers.zn",
            "shared/zk_prover_transaction.zn",
            "shared/utils/utxo_digests.zn",
            "shared/merkle_tree.zn",
            "move/validate/contract_rules.zn",
            "move/main.zn"
        )
    }

    private fun getNextPowerOfTwo(value: Int): Int {
        val highestOneBit = Integer.highestOneBit(value)
        return if (value == 1) {
            2
        } else {
            highestOneBit shl 1
        }
    }

    // TODO: These functions will be removed once Zinc supports dynamic length arrays.
    private fun getGroupSize(constsPath: String): Map<String, Int> {
        val regex = "const[ ]+([A-Z]+)_GROUP_SIZE[a-z,0-9,: ]+=[ ]?([0-9]+)".toRegex()

        return regex.findAll(File(constsPath).readText()).map {
            val (a, b) = it.destructured
            a.toLowerCase() to b.toInt()
        }.toMap()
    }

    fun <K, V> Map<K, V>.single(predicate: (K, V) -> Boolean) =
        filter { (k, v) -> predicate(k, v) }.values.single()

    private fun findCorrespondingMerkleTreeFunction(componentPath: String, circuitPath: String): String {
        val componentGroupSizes = getGroupSize(commandConstsMap.single { command, _ -> circuitPath.contains(command) })
        val componentGroupName =
            componentPath.substring(componentPath.indexOf("components/") + 11, componentPath.indexOf(".zn") - 1)

        if (componentGroupSizes.containsKey(componentGroupName)) {
            val componentGroupSize = componentGroupSizes.getValue(componentGroupName)

            return when {
                // This condition is executed when there is no element in the component group.
                // The return value is allOnesHash
                componentGroupSize == 0 -> {
                    ("""
        // Return all ones hash
        [true; NODE_DIGEST_BITS]
        """)
                }
                // This condition is executed when the defined group size is an exact power of 2.
                // The return value is the merkle tree function that corresponds to the group size.
                componentGroupSize % 2 == 0 -> {
                    ("""
        let component_leaf_hashes = compute_leaf_hashes(this, privacy_salt);
        
        get_merkle_tree_from_${componentGroupSize}_component_group_leaf_digests(component_leaf_hashes)
        """)
                }
                // This condition is executed when the defined group size is not a power of 2.
                // The function finds the next power of 2 and adds padded values to the group.
                // The return value is the merkle tree function that corresponds to the padded group size.
                else -> {
                    val paddedGroupSize = getNextPowerOfTwo(componentGroupSize)
                    ("""
        let component_leaf_hashes = compute_leaf_hashes(this, privacy_salt);
        
        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; $paddedGroupSize];
        for i in 0..$componentGroupSize {
            padded_leaves[i] = component_leaf_hashes[i];
        }
        
        get_merkle_tree_from_${paddedGroupSize}_component_group_leaf_digests(padded_leaves)
        """)
                }
            }
        }
        // This condition is executed when there is no component group size defined.
        // It is possible for notary, timeWindow, parameters groups
        // In that case, we call Merkle tree function for 2 with padded leaves
        return ("""
        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; 2];
        padded_leaves[0] = component_leaf_hash;
        
        get_merkle_tree_from_2_component_group_leaf_digests(padded_leaves)
        """)
    }

    fun build() {
        val circuit = File(circuitPath)
        circuit.parentFile?.mkdirs() // Make sure the parent path for the circuit exists.
        circuit.writeText("//! Combined circuit\n//! GENERATED CODE. DO NOT EDIT\n//! Edit a corresponding constituent\n\n")
        orderedModules
            .filter {
                // Remove modules with the debug functionality if isDbgOn is set to false.
                !it.contains("$modules/shared/debug") || isDbgOn
            }
            .map {
                val part = File(it)

                circuit.appendText("//!  IN ==== $it\n")

                part.readLines()
                    .forEach { line ->
                        if (line.contains("// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###")) {
                            circuit.appendText(findCorrespondingMerkleTreeFunction(it, circuitPath) + "\n")
                        } else {
                            if (isDbgOn || !line.contains("dbg!")) {
                                circuit.appendText("$line\n")
                            }
                        }
                    }

                circuit.appendText("//! OUT ==== $it\n\n")
            }
    }
}
