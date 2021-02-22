package com.ing.zknotary.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CompileZincTask : DefaultTask() {

    @get:Input
    val merkleLeaves = 9

    @get:InputDirectory
    val inputDirectory = "${project.rootDir}/contracts/src/main/zinc/"

    @OutputDirectory
    var outputDirectory = "${project.rootDir}/contracts/build/compileZinc"

    @TaskAction
    fun compileZinc() {
        generateMerkleUtils()
        generateZKContractState()
        generateContractRules()

        println("done")
    }

    @Suppress("LongMethod")
    fun generateMerkleUtils() {
        val merkleUtilsPath = "$inputDirectory/shared/merkle_utils.zn"

        // outputs.file(merkleUtilsPath)
        // circuits.forEach {
        //    inputs.files(it.orderedModules.filter { !it.contains("merkle_utils.zn") })
        // }

        // #######################################################
        // It is of UTTER importance to not reformat code snippets
        // #######################################################

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

    fun generateZKContractState() {

        // TODO: Generate this code from the actual ContractState in zkdapp
        val zkContractStateFile = "$inputDirectory/shared/zk_contract_state.zn"

        val zkContractState = File(zkContractStateFile)
        zkContractState.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zk-notary/gradle-plugin/CompileZincTask.kt\n\n")
        zkContractState.appendText(
            """
const ZKCONTRACT_STATE_BYTES: u16 = PUBKEY_BYTES + U32_BYTES;
const ZKCONTRACT_STATE_FINGERPRINT_BYTES: u16 = PUBKEY_BYTES + U32_BYTES;
const ZKCONTRACT_STATE_FINGERPRINT_BITS: u16 = ZKCONTRACT_STATE_FINGERPRINT_BYTES * BYTE_BITS;

struct ZKContractState {
    owner: Party,
    value: i32,
}

impl ZKContractState {
    fn fingerprint(this: ZKContractState) -> [bool; ZKCONTRACT_STATE_FINGERPRINT_BITS] {
        let mut result = [false; ZKCONTRACT_STATE_FINGERPRINT_BITS];
        //fingerprint data
        //dataOwner_owning_key
        result[0..PUBKEY_FINGERPRINT_BITS] = PubKey::fingerprint(this.owner.owning_key);
        //value
        result[PUBKEY_FINGERPRINT_BITS..(PUBKEY_FINGERPRINT_BITS + U32_BITS)] = to_bits(this.value);
        //fingerprint notary
        //notary_owning_key
        result
    }
}
                """
        )
    }

    fun generateContractRules() {
        // TODO: Implement the actual code generation. This is a temp code to locate files

        val commands = listOf<String>("move", "create")

        commands.forEach {
            val contractRulesFile = "$inputDirectory/$it/contract_rules.zn"
            val contractRules = File(contractRulesFile)
            contractRules.parentFile?.mkdirs()
            contractRules.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zk-notary/gradle-plugin/CompileZincTask.kt\n\n")
            contractRules.appendText(
                """
//TODO: Implement contract rules for $it command                    
                """
            )
        }
    }
}
