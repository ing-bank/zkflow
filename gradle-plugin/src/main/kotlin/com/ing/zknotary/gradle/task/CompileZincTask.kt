package com.ing.zknotary.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CompileZincTask : DefaultTask() {

    @InputFiles
    val zincPlatformSources = "${project.rootDir}/contracts/build/zinc-platform-source"

    @InputFiles
    val zkdappInputFiles = "${project.rootDir}/contracts/src/main/zinc/"

    @OutputDirectory
    var outputCircuitDirectory = "${project.rootDir}/contracts/build/circuits/"

    @TaskAction
    fun compileZinc() {
        val create = CommandCircuit.Create(zincPlatformSources, zkdappInputFiles, outputCircuitDirectory)
        val move = CommandCircuit.Move(zincPlatformSources, zkdappInputFiles, outputCircuitDirectory)

        val circuits: List<CommandCircuit> = listOf(create, move)

        circuits.forEach { circuit ->
            circuit.build()
            circuit.generateZargo()
            // Compile circuit
            val circuitPath = File(circuit.zargoRoot)
            project.exec {
                it.workingDir = circuitPath
                it.executable = "zargo"
                it.args = listOf("clean", "-v")
            }
        }
    }

    sealed class CommandCircuit(command: String, zincPlatformRoot: String, zkdappZincRoot: String, outputRoot: String) {
        val coreZincModules = zincPlatformRoot
        val zkdappZincModules = zkdappZincRoot
        val outputCircuitRoot = outputRoot

        val isDbgOn = false

        // Combine modules in a given order,
        abstract val circuitOrderedModules: List<String>
        val orderedModules: Set<String>
            get() = circuitOrderedModules.map { it }.toSet()

        // write the result to this file.
        val circuitPath = "$outputCircuitRoot/$command/src/main.zn"

        // a map where each command has its corresponding consts path
        val commandConstsMap = mapOf(
            "create" to "$zkdappZincModules/create/utils/consts.zn",
            "move" to "$zkdappZincModules/move/utils/consts.zn"
        )

        val zargoRoot = "$outputCircuitRoot/$command"
        val zargoPath = "$zargoRoot/Zargo.toml"
        class Create(coreZincModules: String, zkdappZincModules: String, outputCircuitRoot: String) : CommandCircuit("create", coreZincModules, zkdappZincModules, outputCircuitRoot) {
            override val circuitOrderedModules = listOf(
                "$coreZincModules/utils/preamble.zn",
                "$coreZincModules/utils/consts.zn",
                "$zkdappZincModules/create/utils/consts.zn",
                "$coreZincModules/component_group_enum.zn",
                "$coreZincModules/crypto/privacy_salt.zn",
                "$coreZincModules/dto/component_group_leaf_digest_dto.zn",
                "$coreZincModules/dto/node_digest_dto.zn",
                "$coreZincModules/dto/nonce_digest_dto.zn",
                "$coreZincModules/debug/debug_utils.zn",
                "$coreZincModules/utils/crypto_utils.zn",
                "$coreZincModules/crypto/pub_key.zn",
                "$zkdappZincModules/shared/merkle_utils.zn",
                "$coreZincModules/party.zn",
                "$zkdappZincModules/shared/zk_contract_state.zn",
                "$coreZincModules/state_and_ref.zn",
                "$coreZincModules/components/inputs.zn",
                "$coreZincModules/components/outputs.zn",
                "$coreZincModules/components/references.zn",
                "$coreZincModules/components/commands.zn",
                "$coreZincModules/components/attachments.zn",
                "$coreZincModules/components/notary.zn",
                "$coreZincModules/components/time_window.zn",
                "$coreZincModules/components/parameters.zn",
                "$coreZincModules/components/signers.zn",
                "$coreZincModules/zk_prover_transaction.zn",
                "$coreZincModules/utils/utxo_digests.zn",
                "$coreZincModules/merkle_tree.zn",
                "$zkdappZincModules/create/validate/contract_rules.zn",
                "$zkdappZincModules/create/main.zn"
            )
        }

        class Move(coreZincModules: String, zkdappZincModules: String, outputCircuitRoot: String) : CommandCircuit("move", coreZincModules, zkdappZincModules, outputCircuitRoot) {
            override val circuitOrderedModules = listOf(
                "$coreZincModules/utils/preamble.zn",
                "$coreZincModules/utils/consts.zn",
                "$zkdappZincModules/move/utils/consts.zn",
                "$coreZincModules/component_group_enum.zn",
                "$coreZincModules/crypto/privacy_salt.zn",
                "$coreZincModules/dto/component_group_leaf_digest_dto.zn",
                "$coreZincModules/dto/node_digest_dto.zn",
                "$coreZincModules/dto/nonce_digest_dto.zn",
                "$coreZincModules/debug/debug_utils.zn",
                "$coreZincModules/utils/crypto_utils.zn",
                "$coreZincModules/crypto/pub_key.zn",
                "$zkdappZincModules/shared/merkle_utils.zn",
                "$coreZincModules/party.zn",
                "$zkdappZincModules/shared/zk_contract_state.zn",
                "$coreZincModules/state_and_ref.zn",
                "$coreZincModules/components/inputs.zn",
                "$coreZincModules/components/outputs.zn",
                "$coreZincModules/components/references.zn",
                "$coreZincModules/components/commands.zn",
                "$coreZincModules/components/attachments.zn",
                "$coreZincModules/components/notary.zn",
                "$coreZincModules/components/time_window.zn",
                "$coreZincModules/components/parameters.zn",
                "$coreZincModules/components/signers.zn",
                "$coreZincModules/zk_prover_transaction.zn",
                "$coreZincModules/utils/utxo_digests.zn",
                "$coreZincModules/merkle_tree.zn",
                "$zkdappZincModules/move/validate/contract_rules.zn",
                "$zkdappZincModules/move/main.zn"
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
                        (
                            """
        // Return all ones hash
        [true; NODE_DIGEST_BITS]
        """
                            )
                    }
                    // This condition is executed when the defined group size is an exact power of 2.
                    // The return value is the merkle tree function that corresponds to the group size.
                    componentGroupSize % 2 == 0 -> {
                        (
                            """
        let component_leaf_hashes = compute_leaf_hashes(this, privacy_salt);
        
        get_merkle_tree_from_${componentGroupSize}_component_group_leaf_digests(component_leaf_hashes)
        """
                            )
                    }
                    // This condition is executed when the defined group size is not a power of 2.
                    // The function finds the next power of 2 and adds padded values to the group.
                    // The return value is the merkle tree function that corresponds to the padded group size.
                    else -> {
                        val paddedGroupSize = getNextPowerOfTwo(componentGroupSize)
                        (
                            """
        let component_leaf_hashes = compute_leaf_hashes(this, privacy_salt);
        
        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; $paddedGroupSize];
        for i in 0..$componentGroupSize {
            padded_leaves[i] = component_leaf_hashes[i];
        }
        
        get_merkle_tree_from_${paddedGroupSize}_component_group_leaf_digests(padded_leaves)
        """
                            )
                    }
                }
            }
            // This condition is executed when there is no component group size defined.
            // It is possible for notary, timeWindow, parameters groups
            // In that case, we call Merkle tree function for 2 with padded leaves
            return (
                """
        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; 2];
        padded_leaves[0] = component_leaf_hash;
        
        get_merkle_tree_from_2_component_group_leaf_digests(padded_leaves)
        """
                )
        }

        fun build() {
            val circuit = File(circuitPath)
            circuit.parentFile?.mkdirs() // Make sure the parent path for the circuit exists.
            circuit.writeText("//! Combined circuit\n//! GENERATED CODE. DO NOT EDIT\n//! Edit a corresponding constituent\n\n")
            orderedModules
                .filter {
                    // Remove modules with the debug functionality if isDbgOn is set to false.
                    !it.contains("$coreZincModules/src/main/resources/zinc-platform-source/debug") || isDbgOn
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

        fun generateZargo() {
            val zargo = File(zargoPath)
            zargo.parentFile?.mkdirs() // Make sure the parent path for the circuit exists.
            zargo.writeText(
                """
[circuit]
name = "${this::class.simpleName} command"
version = "0.1.0"                
            """
            )
        }
    }
}
