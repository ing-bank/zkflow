package com.ing.zknotary.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CreateZincFilesTask : DefaultTask() {

    @Input
    val merkleLeaves = 9

    @Input
    val circuits = listOf("move", "create")

    @InputFiles
    val zkdappInputFiles = "${project.rootDir}/contracts/src/main/zinc/"

    @TaskAction
    fun createZincFiles() {
        generateMerkleUtils()
        createZKContractState()
        createContractRules()
        createConsts()
        createMain()
    }

    @Suppress("LongMethod")
    fun generateMerkleUtils() {
        val merkleUtilsPath = "$zkdappInputFiles/shared/merkle_utils.zn"

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
        merkleUtils.parentFile?.mkdirs()
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

    fun createZKContractState() {

        // TODO: Generate this code from the actual ContractState in zkdapp
        val zkContractStateFile = "$zkdappInputFiles/shared/zk_contract_state.zn"

        val zkContractState = File(zkContractStateFile)
        zkContractState.writeText("//TODO: Implement ZKContractState from the actual ContractState in zkdapp\n")
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

    fun createContractRules() {
        circuits.forEach {
            // create validate/contract_rules.zn
            val filePath = "$zkdappInputFiles/$it/validate/contract_rules.zn"
            val fileContent = File(filePath)
            fileContent.parentFile?.mkdirs()
            fileContent.writeText(
                """//TODO: Implement contract rules validation for $it command                    
                """
            )
        }
    }

    fun createConsts() {
        circuits.forEach {
            // create utils/consts.zn
            val filePath = "$zkdappInputFiles/$it/utils/consts.zn"
            val fileContent = File(filePath)
            fileContent.parentFile?.mkdirs()
            fileContent.writeText("// TODO: Provide the actual component group size for each component group of $it command.\n")
            fileContent.appendText(
                """
//!
//! Sizes of Corda's component groups are fixed to the below values.
//! Corda must produce witness such it contains the expected number of components
//! in each group.
//! See definition of ZKCommandData on the Corda side.
//!
const ATTACHMENT_GROUP_SIZE: u16 = //TODO
const COMMAND_GROUP_SIZE: u16 = //TODO
const INPUT_GROUP_SIZE: u16 = //TODO
const OUTPUT_GROUP_SIZE: u16 = //TODO
const REFERENCE_GROUP_SIZE: u16 = //TODO
const SIGNER_GROUP_SIZE: u16 = //TODO
                """
            )
        }
    }

    @Suppress("LongMethod")
    fun createMain() {
        circuits.forEach {
            // create main.zn
            val filePath = "$zkdappInputFiles/$it/main.zn"
            val fileContent = File(filePath)
            fileContent.parentFile?.mkdirs()
            fileContent.writeText(
                """
//!
//! The '$it' main module.
//!

struct PublicInput {
    transaction_id: NodeDigestDto,
    input_hashes: [ComponentGroupLeafDigestDto; INPUT_GROUP_SIZE],
    reference_hashes: [ComponentGroupLeafDigestDto; REFERENCE_GROUP_SIZE],
}

fn main(witness: Witness) -> PublicInput {
    // Check contract rules
    check_contract_rules(witness.transaction);

    // Compute the transaction id
    let root_hash = build_merkle_tree(witness.transaction);
"""
            )
            when (it) {
                "move" -> {
                    fileContent.appendText(
                        """
    PublicInput {
        transaction_id: NodeDigestDto::from_bits_to_bytes(root_hash),        
        input_hashes: compute_input_utxo_digests(
            witness.transaction.inputs.components,
            witness.input_nonces,
        ),
        reference_hashes: compute_reference_utxo_digests(
            witness.transaction.references.components,
            witness.reference_nonces,
        ),
    }
}
                        """
                    )
                }
                "create" -> {
                    fileContent.appendText(
                        """
    PublicInput {
        transaction_id: NodeDigestDto::from_bits_to_bytes(root_hash),        
        input_hashes: [ComponentGroupLeafDigestDto {
            bytes: [0; COMPONENT_GROUP_LEAF_DIGEST_BYTES],
        }; INPUT_GROUP_SIZE],
        reference_hashes: compute_reference_utxo_digests(
            witness.transaction.references.components,
            witness.reference_nonces,
        ),
    }
}
                        """
                    )
                }
            }
        }
    }
}
