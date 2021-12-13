package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.nonceDigest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.secureHash
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.stateRef
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.timeWindow
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.ATTACHMENTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.COMMANDS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUT_NONCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.NOTARY
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PARAMETERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PRIVACY_SALT
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCE_NONCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_INPUT_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SERIALIZED_REFERENCE_UTXOS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.TIME_WINDOW
import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

class LedgerTransactionFactory {
// // TODO uncomment this block when combining StateRefs with the UTXOs into StateAndRefs
// (
//    private val zincTypeResolver: ZincTypeResolver,
// ) {
//    private val standardTypes = StandardTypes(zincTypeResolver)

    fun createLedgerTransaction(
        inputs: Map<KClass<out ContractState>, Int>,
//        // TODO uncomment this block when combining StateRefs with the UTXOs into StateAndRefs
//        outputs: Map<KClass<out ContractState>, Int>,
        references: Map<KClass<out ContractState>, Int>,
        transactionMetadata: ResolvedZKTransactionMetadata,
        witness: Witness,
    ): BflStruct {
//        // TODO uncomment this block when combining StateRefs with the UTXOs into StateAndRefs
//        val inputGroup = structWithStateAndRefs("InputGroup", inputs)
//        val outputGroup = structWithTransactionStates(outputs)
//        val referencesGroup = structWithStateAndRefs("ReferenceGroup", references)

        return struct {
            name = LEDGER_TRANSACTION
//            // TODO uncomment this block when combining StateRefs with the UTXOs into StateAndRefs
//            field { name = "resolved_" + INPUTS; type = inputGroup }
//            field { name = "resolved_" + OUTPUTS; type = outputGroup }
//            field { name = "resolved_" + REFERENCES; type = referencesGroup }
            field {
                name = INPUTS; type = array {
                    capacity = transactionMetadata.numberOfInputs
                    elementType = stateRef
                }
            }
            field { name = OUTPUTS; type = witness.serializedOutputGroup.deserializedStruct }
            field {
                name = REFERENCES; type = array {
                    capacity = transactionMetadata.numberOfReferences
                    elementType = stateRef
                }
            }
            field { name = COMMANDS; type = CommandGroupFactory.commandsGroup(transactionMetadata, witness.signerModule) }
            field { name = ATTACHMENTS; type = array { capacity = transactionMetadata.attachmentCount; elementType = secureHash } }
            field { name = NOTARY; type = witness.notaryModule }
            if (transactionMetadata.hasTimeWindow) {
                field { name = TIME_WINDOW; type = timeWindow }
            }
            field { name = PARAMETERS; type = secureHash }
            field { name = SIGNERS; type = array { capacity = transactionMetadata.numberOfSigners; elementType = witness.signerModule } }
            field { name = PRIVACY_SALT + "_field"; type = privacySalt }
            field { name = INPUT_NONCES; type = array { capacity = inputs.values.sum(); elementType = nonceDigest } }
            field { name = REFERENCE_NONCES; type = array { capacity = references.values.sum(); elementType = nonceDigest } }
            field { name = SERIALIZED_INPUT_UTXOS; type = witness.serializedInputUtxos.deserializedStruct }
            field { name = SERIALIZED_REFERENCE_UTXOS; type = witness.serializedReferenceUtxos.deserializedStruct }
            isDeserializable = false
        }
    }

//    // TODO uncomment this block when combining StateRefs with the UTXOs into StateAndRefs
//    private fun structWithStateAndRefs(groupName: String, inputs: Map<KClass<out ContractState>, Int>) =
//        struct {
//            name = groupName
//            inputs.forEach {
//                field {
//                    name = it.key.simpleName!!.camelToSnakeCase() // TODO use utility function get contract class name
//                    type = list {
//                        capacity = it.value
//                        elementType = standardTypes.stateAndRef(zincTypeResolver.zincTypeOf(it.key))
//                    }
//                }
//            }
//            isDeserializable = false
//        }
//
//    private fun structWithTransactionStates(inputs: Map<KClass<out ContractState>, Int>) =
//        struct {
//            name = "OutputGroup"
//            inputs.forEach {
//                field {
//                    name = it.key.simpleName!!.camelToSnakeCase()
//                    type = list {
//                        capacity = it.value
//                        elementType = standardTypes.transactionState(zincTypeResolver.zincTypeOf(it.key))
//                    }
//                }
//            }
//            isDeserializable = false
//        }

    companion object {
        const val LEDGER_TRANSACTION = "LedgerTransaction"
    }
}
