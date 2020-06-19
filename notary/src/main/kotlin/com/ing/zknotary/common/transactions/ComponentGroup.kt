package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.states.ZKStateRef
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.lazyMapped
import net.corda.core.node.services.AttachmentId
import net.corda.core.serialization.SerializedBytes
import net.corda.core.transactions.ComponentGroup

fun MutableList<ComponentGroup>.addInputsGroup(
    inputs: List<ZKStateRef>,
    serializer: (Any, Int) -> SerializedBytes<Any>
) {
    if (inputs.isNotEmpty()) this.add(
        ComponentGroup(ComponentGroupEnum.INPUTS_GROUP.ordinal, inputs.lazyMapped(serializer))
    )
}

fun MutableList<ComponentGroup>.addReferencesGroup(
    references: List<ZKStateRef>,
    serializer: (Any, Int) -> SerializedBytes<Any>
) {
    if (references.isNotEmpty()) this.add(
        ComponentGroup(ComponentGroupEnum.REFERENCES_GROUP.ordinal, references.lazyMapped(serializer))
    )
}

fun MutableList<ComponentGroup>.addOutputsGroup(
    outputs: List<ZKStateRef>,
    serializer: (Any, Int) -> SerializedBytes<Any>
) {
    if (outputs.isNotEmpty()) this.add(
        ComponentGroup(ComponentGroupEnum.OUTPUTS_GROUP.ordinal, outputs.lazyMapped(serializer))
    )
}

fun MutableList<ComponentGroup>.addAttachmentsGroup(
    attachments: List<AttachmentId>,
    serializer: (Any, Int) -> SerializedBytes<Any>
) {
    if (attachments.isNotEmpty()) this.add(
        ComponentGroup(
            ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal,
            attachments.lazyMapped(serializer)
        )
    )
}

fun MutableList<ComponentGroup>.addNotaryGroup(
    notary: Party,
    serializer: (Any, Int) -> SerializedBytes<Any>
) {
    this.add(
        ComponentGroup(
            ComponentGroupEnum.NOTARY_GROUP.ordinal,
            listOf(notary).lazyMapped(serializer)
        )
    )
}

fun MutableList<ComponentGroup>.addTimeWindowGroup(
    timeWindow: TimeWindow?,
    serializer: (Any, Int) -> SerializedBytes<Any>
) {
    if (timeWindow != null) this.add(
        ComponentGroup(
            ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
            listOf(timeWindow).lazyMapped(serializer)
        )
    )
}

fun MutableList<ComponentGroup>.addCommandGroup(
    command: Command<*>,
    serializer: (Any, Int) -> SerializedBytes<Any>
) {
    // TODO: is it still necessary to split this for filtering purposes like in standard Corda?
    this.add(
        ComponentGroup(
            ComponentGroupEnum.COMMANDS_GROUP.ordinal,
            listOf(command.value).lazyMapped(serializer)
        )
    )
    this.add(
        ComponentGroup(
            ComponentGroupEnum.SIGNERS_GROUP.ordinal,
            listOf(command.signers).lazyMapped(serializer)
        )
    )
}

fun MutableList<ComponentGroup>.addNetWorkParametersHashGroup(
    networkParametersHash: SecureHash?,
    serializer: (Any, Int) -> SerializedBytes<Any>
) {
    if (networkParametersHash != null) this.add(
        ComponentGroup(
            ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
            listOf(networkParametersHash).lazyMapped(serializer)
        )
    )
}
