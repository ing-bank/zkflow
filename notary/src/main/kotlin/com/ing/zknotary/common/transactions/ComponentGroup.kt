package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.states.ZKStateRef
import com.ing.zknotary.common.zkp.fingerprint
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.transactions.ComponentGroup
import net.corda.core.utilities.OpaqueBytes
import java.security.PublicKey

fun MutableList<ComponentGroup>.addInputsGroup(inputs: List<ZKStateRef>) {
    if (inputs.isNotEmpty()) this.add(
        ComponentGroup(
            ComponentGroupEnum.INPUTS_GROUP.ordinal,
            inputs.map { OpaqueBytes(it.fingerprint) }
        )
    )
}

fun MutableList<ComponentGroup>.addReferencesGroup(references: List<ZKStateRef>) {
    if (references.isNotEmpty()) this.add(
        ComponentGroup(
            ComponentGroupEnum.REFERENCES_GROUP.ordinal,
            references.map { OpaqueBytes(it.fingerprint) }
        )
    )
}

fun MutableList<ComponentGroup>.addOutputsGroup(outputs: List<ZKStateRef>) {
    if (outputs.isNotEmpty()) this.add(
        ComponentGroup(
            ComponentGroupEnum.OUTPUTS_GROUP.ordinal,
            outputs.map { OpaqueBytes(it.fingerprint) }
        )
    )
}

fun MutableList<ComponentGroup>.addAttachmentsGroup(attachments: List<AttachmentId>) {
    if (attachments.isNotEmpty()) this.add(
        ComponentGroup(
            ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal,
            attachments.map { OpaqueBytes(it.fingerprint) }
        )
    )
}

fun MutableList<ComponentGroup>.addNotaryGroup(
    notary: Party,
    digestService: DigestService
) {
    this.add(
        ComponentGroup(
            ComponentGroupEnum.NOTARY_GROUP.ordinal,
            listOf(notary).map { digestService.hash(it.fingerprint) }
        )
    )
}

fun MutableList<ComponentGroup>.addTimeWindowGroup(
    timeWindow: TimeWindow?,
    digestService: DigestService
) {
    if (timeWindow != null) this.add(
        ComponentGroup(
            ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
            listOf(timeWindow).map { digestService.hash(it.fingerprint) }
        )
    )
}

fun MutableList<ComponentGroup>.addCommandGroup(
    command: ZKCommandData,
    digestService: DigestService
) {
    this.add(
        ComponentGroup(
            ComponentGroupEnum.COMMANDS_GROUP.ordinal,
            listOf(command).map { digestService.hash(it.fingerprint) }
        )
    )
}

fun MutableList<ComponentGroup>.addCommandSignersGroup(
    signers: List<PublicKey>,
    digestService: DigestService
) {
    this.add(
        ComponentGroup(
            ComponentGroupEnum.SIGNERS_GROUP.ordinal,
            signers.map { digestService.hash(it.fingerprint) }
        )
    )
}

fun MutableList<ComponentGroup>.addNetWorkParametersHashGroup(networkParametersHash: SecureHash?) {
    if (networkParametersHash != null) this.add(
        ComponentGroup(
            ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
            listOf(networkParametersHash).map { OpaqueBytes(it.fingerprint) }
        )
    )
}
