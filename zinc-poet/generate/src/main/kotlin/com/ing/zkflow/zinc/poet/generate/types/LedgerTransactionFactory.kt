package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.nonceDigest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.secureHash
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.timeWindow
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.COMMANDS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.INPUT_NONCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.NOTARY
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.OUTPUTS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PARAMETERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.PRIVACY_SALT
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.REFERENCE_NONCES
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.TIME_WINDOW

class LedgerTransactionFactory(
    private val commandGroupFactory: CommandGroupFactory,
    private val standardTypes: StandardTypes,
) {
    fun createLedgerTransaction(
        inputGroup: BflModule,
        referencesGroup: BflModule,
        commandMetadata: ResolvedZKCommandMetadata,
        witness: Witness,
    ): BflStruct = struct {
        name = LEDGER_TRANSACTION
        if (commandMetadata.privateInputs.isNotEmpty()) {
            field { name = INPUTS; type = inputGroup }
        }
        if (commandMetadata.privateOutputs.isNotEmpty()) {
            field { name = OUTPUTS; type = witness.serializedOutputGroup.deserializedStruct }
        }
        if (commandMetadata.privateReferences.isNotEmpty()) {
            field { name = REFERENCES; type = referencesGroup }
        }
        field { name = COMMANDS; type = commandGroupFactory.createCommandGroup(commandMetadata) }
//        if (commandMetadata.attachmentCount > 0) {
//            field {
//                name = ATTACHMENTS; type =
//                    array { capacity = commandMetadata.attachmentCount; elementType = secureHash }
//            }
//        }
        field { name = NOTARY; type = standardTypes.notaryModule }
        if (commandMetadata.timeWindow) {
            field { name = TIME_WINDOW; type = timeWindow }
        }
        field { name = PARAMETERS; type = secureHash }
        field { name = SIGNERS; type = array { capacity = commandMetadata.numberOfSigners; elementType = standardTypes.signerModule } }
        field { name = PRIVACY_SALT + "_field"; type = privacySalt }
        if (commandMetadata.privateInputs.isNotEmpty()) {
            field {
                name = INPUT_NONCES; type =
                    array { capacity = commandMetadata.privateInputs.size; elementType = nonceDigest }
            }
        }
        if (commandMetadata.privateReferences.isNotEmpty()) {
            field {
                name = REFERENCE_NONCES; type =
                    array { capacity = commandMetadata.privateReferences.size; elementType = nonceDigest }
            }
        }
        isDeserializable = false
    }

    companion object {
        const val LEDGER_TRANSACTION = "LedgerTransaction"
    }
}
