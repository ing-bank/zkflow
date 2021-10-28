package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.compilation.zinc.util.CircuitConfigurator
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import com.ing.zkflow.util.camelToSnakeCase
import java.io.File

data class TxStateTemplateParameters(val state: CircuitConfigurator.State) : TemplateParameters(
    "tx_state.zn",
    listOf(
        AbstractPartyTemplateParameters.selectPartyParameters(state.notaryKeySchemeCodename),
        AttachmentConstraintTemplateParameters.selectParameters(state.attachmentConstraint).polymorphic(),
        ByteArrayTemplateParameters(CordaSerializers.CLASS_NAME_SIZE),
        NullableTemplateParameters("nullable_integer.zn", null, "i32")
    )
) {
    override val typeName = state.zincType

    override fun getModuleName() = File(state.location).name.removeSuffix(".zn")

    override fun getTargetFilename(): String =
        "tx_state_${typeName.camelToSnakeCase()}.zn"

    override fun getReplacements() =
        getTypeReplacements("USER_STATE_") +
            AbstractPartyTemplateParameters
                .selectPartyParameters(state.notaryKeySchemeCodename)
                .getTypeReplacements("PARTY_") +
            AttachmentConstraintTemplateParameters
                .selectParameters(state.attachmentConstraint)
                .polymorphic()
                .getTypeReplacements("ATTACHMENT_CONSTRAINT_") +
            ByteArrayTemplateParameters(CordaSerializers.CLASS_NAME_SIZE)
                .getTypeReplacements("CLASS_NAME_")
}