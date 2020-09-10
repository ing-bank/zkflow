val circuitRoot = "prover/ZKMerkleTree"

val modulesPath = "$circuitRoot/modules"

val circuitPath = "$circuitRoot/src/main.zn"

val isDbgOn = false

val merkleLeaves = 9
val merkleUtilsPath = "utils/merkle_utils.zn"

val combineInOrder = listOf(
    "utils/preamble.zn",
    "utils/consts.zn",
    "component_group_enum.zn",
    "crypto/privacy_salt.zn",
    "crypto/nonce.zn",
    "crypto/component_group_leaf_digest.zn",
    "crypto/node_digest.zn",
    "crypto/pub_key.zn",
    "utils/crypto_utils.zn",
    merkleUtilsPath,
    "zk_state_and_ref.zn",
    "components/inputs.zn",
    "components/outputs.zn",
    "components/references.zn",
    "components/commands.zn",
    "components/attachments.zn",
    "components/notary.zn",
    "components/time_window.zn",
    "components/parameters.zn",
    "components/signers.zn",
    "zk_prover_transaction.zn",
    "merkle_tree.zn",
    "validate/contract_rules.zn",
    "main.zn"
)


task("circuit") {
    dependsOn("merkleUtils", "rustfmtCheck", "buildCircuit")
}

task<Exec>("rustfmt") {
    combineInOrder.map { commandLine("rustfmt", "--check", File("$modulesPath/$it").absolutePath) }
}

task<Exec>("rustfmtCheck") {
    combineInOrder.map { commandLine("rustfmt", "--check", File("$modulesPath/$it").absolutePath) }
}

task("merkleUtils") {
    fun isPow2(num: Int) = num and (num - 1) == 0

    val fullLeaves = run {
        var l = merkleLeaves
        while (!isPow2(l)) { l++ }
        l
    }

    val merkleUtils = File("$modulesPath/$merkleUtilsPath")
    println(merkleUtils.absolutePath)
    merkleUtils.writeText("//! Limited-depth recursion for Merkle tree construction\n")
    merkleUtils.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in prover/build.gradle.kts\n\n");

    merkleUtils.appendText("//! Merkle tree construction for NodeDigestBits")
    merkleUtils.appendText(
        """
fn get_merkle_tree_from_2_node_digests(leaves: [NodeDigestBits; 2]) -> NodeDigestBits {
    dbg!("Consuming 2 leaves");
    dbg!("0: {}", NodeDigest::from_bits(leaves[0]));
    dbg!("1: {}", NodeDigest::from_bits(leaves[1]));
    pedersen_to_padded_bits(
        pedersen(NodeDigest::concatenate(leaves[0], leaves[1])).0,
    )
}
""")

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
            pedersen(NodeDigest::concatenate(leaves[2 * i], leaves[2 * i + 1])).0,
        );
        dbg!("{}: {}", 2 * i, NodeDigest::from_bits(leaves[2 * i]));
        dbg!("{}: {}", 2 * i + 1, NodeDigest::from_bits(leaves[2 * i + 1]));
        dbg!("Digest: {}", NodeDigest::from_bits(new_leaves[i]));
    }
    dbg!("");
    get_merkle_tree_from_${levelUp}_node_digests(new_leaves)
}
"""
        )
        leaves *= 2
    } while (leaves <= fullLeaves)

    merkleUtils.appendText("\n//! Merkle tree construction for ComponentGroupLeafDigestBits.\n" +
        "//! Use it only for the computation of a component sub-Merkle tree from component group leaf hashes.")
    merkleUtils.appendText(
        """
fn get_merkle_tree_from_2_component_group_leaf_digests(leaves: [ComponentGroupLeafDigest; 2]) -> NodeDigestBits {
    dbg!("Consuming 2 leaves");
    dbg!("0: {}", ComponentGroupLeafDigest::to_bytes(leaves[0]));
    dbg!("1: {}", ComponentGroupLeafDigest::to_bytes(leaves[1]));
    pedersen_to_padded_bits(
        pedersen(ComponentGroupLeafDigest::concatenate(leaves[0], leaves[1])).0,
    )
}
""")

    leaves = 4

    do {
        val levelUp = leaves / 2

        merkleUtils.appendText(
            """
fn get_merkle_tree_from_${leaves}_component_group_leaf_digests(leaves: [ComponentGroupLeafDigest; $leaves]) -> NodeDigestBits {
    dbg!("Consuming $leaves leaves");
    let mut new_leaves = [[false; NODE_DIGEST_BITS]; $levelUp];
    for i in 0..$levelUp {
        new_leaves[i] = pedersen_to_padded_bits(
            pedersen(ComponentGroupLeafDigest::concatenate(leaves[2 * i], leaves[2 * i + 1])).0,
        );
        dbg!("{}: {}", 2 * i, ComponentGroupLeafDigest::to_bytes(leaves[2 * i]));
        dbg!("{}: {}", 2 * i + 1, ComponentGroupLeafDigest::to_bytes(leaves[2 * i + 1]));
        dbg!("Digest: {}", ComponentGroupLeafDigest::to_bytes(new_leaves[i]));
    }
    dbg!("");
    get_merkle_tree_from_${levelUp}_node_digests(new_leaves)
}
"""
        )
        leaves *= 2
    } while (leaves <= fullLeaves)

    merkleUtils.appendText(
        """
//! Top-level function to be called.
//! Pads the configure number of leaves to the right amount with zero hashes from the right
//! and calls appropriate tree-constructing procedure
fn merkle_root(leaves: [NodeDigestBits; $merkleLeaves]) -> NodeDigestBits {
    dbg!("Building a tree from $merkleLeaves leaves");

    dbg!("Padding from the right with ${fullLeaves - merkleLeaves} zero leaves");
    let mut full_leaves = [[false; NODE_DIGEST_BITS]; $fullLeaves];
    for i in 0..$merkleLeaves {
        full_leaves[i] = leaves[i];
    }

    dbg!("Constructing the root");
    get_merkle_tree_from_${fullLeaves}_node_digests(full_leaves)
}
""")
}

task("buildCircuit") {
    val circuit = File(circuitPath)
    circuit.parentFile?.mkdirs() // Make sure the parent path for the circuit exists.
    circuit.writeText("//! Combined circuit\n//! GENERATED CODE. DO NOT EDIT\n//! Edit a corresponding constituent\n\n");
    combineInOrder.map {
        val part = File("$modulesPath/$it")

        circuit.appendText("//!  IN ==== ${part.absolutePath}\n")

        if (isDbgOn) {
            circuit.appendBytes(part.readBytes())
        } else {
            part
                .readLines()
                .filter { line -> !line.contains("dbg!") }
                .forEach { line -> circuit.appendText("$line\n")}
        }

        circuit.appendText("//! OUT ==== ${part.absolutePath}\n\n")
    }
}