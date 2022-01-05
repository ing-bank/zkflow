package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.nonceDigest
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.privacySalt
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes.Companion.secureHash
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
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.SIGNERS
import com.ing.zkflow.zinc.poet.generate.types.Witness.Companion.TIME_WINDOW

class LedgerTransactionFactory(
    private val commandGroupFactory: CommandGroupFactory,
    private val standardTypes: StandardTypes,
) {
    fun createLedgerTransaction(
        inputGroup: BflModule,
        referencesGroup: BflModule,
        transactionMetadata: ResolvedZKTransactionMetadata,
        witness: Witness,
    ): BflStruct = struct {
        name = LEDGER_TRANSACTION
        field { name = INPUTS; type = inputGroup }
        field { name = OUTPUTS; type = witness.serializedOutputGroup.deserializedStruct }
        field { name = REFERENCES; type = referencesGroup }
        field { name = COMMANDS; type = commandGroupFactory.createCommandGroup(transactionMetadata) }
        field { name = ATTACHMENTS; type = array { capacity = transactionMetadata.attachmentCount; elementType = secureHash } }
        field { name = NOTARY; type = standardTypes.notaryModule }
        if (transactionMetadata.hasTimeWindow) {
            field { name = TIME_WINDOW; type = timeWindow }
        }
        field { name = PARAMETERS; type = secureHash }
        field { name = SIGNERS; type = array { capacity = transactionMetadata.numberOfSigners; elementType = standardTypes.signerModule } }
        field { name = PRIVACY_SALT + "_field"; type = privacySalt }
        field { name = INPUT_NONCES; type = array { capacity = transactionMetadata.numberOfInputs; elementType = nonceDigest } }
        field { name = REFERENCE_NONCES; type = array { capacity = transactionMetadata.numberOfReferences; elementType = nonceDigest } }
        isDeserializable = false
    }

    companion object {
        const val LEDGER_TRANSACTION = "LedgerTransaction"
    }
}
