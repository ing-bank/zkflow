package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.common.transactions.qualifiedConstraintClassName
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ZincType
import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import com.ing.zkflow.util.camelToSnakeCase
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.File

@SuppressFBWarnings("PATH_TRAVERSAL_IN", justification = "WONTFIX: will be thrown away anyway soon for ZincPoet")
data class TxStateTemplateParameters(val metadata: ResolvedZKCommandMetadata, val typeInfo: ZincType) :
    TemplateParameters(
        "tx_state.zn",
        listOf(
            AbstractPartyTemplateParameters.selectPartyParameters(metadata.network.notary.signatureScheme.schemeCodeName),
            AttachmentConstraintTemplateParameters.selectParameters(
                metadata.network.attachmentConstraintType.qualifiedConstraintClassName
            ).polymorphic(),
            ByteArrayTemplateParameters(CordaSerializers.CLASS_NAME_SIZE),
            NullableTemplateParameters("nullable_integer.zn", null, "i32")
        )
    ) {
    override val typeName = typeInfo.typeName

    override fun getModuleName() = File(typeInfo.fileName).name.removeSuffix(".zn")

    override fun getTargetFilename(): String =
        "tx_state_${typeName.camelToSnakeCase()}.zn"

    override fun getReplacements() =
        getTypeReplacements("USER_STATE_") +
            AbstractPartyTemplateParameters
                .selectPartyParameters(metadata.network.notary.signatureScheme.schemeCodeName)
                .getTypeReplacements("PARTY_") +
            AttachmentConstraintTemplateParameters
                .selectParameters(metadata.network.attachmentConstraintType.qualifiedConstraintClassName)
                .polymorphic()
                .getTypeReplacements("ATTACHMENT_CONSTRAINT_") +
            ByteArrayTemplateParameters(CordaSerializers.CLASS_NAME_SIZE)
                .getTypeReplacements("CLASS_NAME_")
}
