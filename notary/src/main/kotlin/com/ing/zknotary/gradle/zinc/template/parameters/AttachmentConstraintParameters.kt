package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class AttachmentConstraintParameters(
    val baseTemplateFile: String,
    val implementationName: String,
    val secureHashTemplateParameters: SecureHashTemplateParameters? = null,
    val pkTemplateParameters: PublicKeyTemplateParameters? = null
) : TemplateParameters(
    baseTemplateFile,
    listOfNotNull(
        StringTemplateParameters(1),
        secureHashTemplateParameters,
        pkTemplateParameters
    )
) {
    private val pkTypeName by lazy { pkTemplateParameters?.typeName }
    private val algName by lazy { pkTypeName?.removeSuffix("PublicKey") ?: "" }
    override val typeName by lazy { "$implementationName$algName" }

    override fun getReplacements() = getTypeReplacements() + (pkTemplateParameters?.getTypeReplacements("PK_") ?: emptyMap())

    override fun getTargetFilename() = getFileName()

    companion object {
        private const val ATTACHMENT_CONSTRAINT_TEMPLATE = "attachment_constraint.zn"
        private const val ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_SECURE_HASH = "attachment_constraint_with_secure_hash.zn"
        private const val ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_PUBLIC_KEY = "attachment_constraint_with_public_key.zn"

        val all = listOf(
            AttachmentConstraintParameters(ATTACHMENT_CONSTRAINT_TEMPLATE, "AlwaysAcceptAttachmentConstraint"),
            AttachmentConstraintParameters(
                baseTemplateFile = ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_SECURE_HASH,
                implementationName = "HashAttachmentConstraint",
                secureHashTemplateParameters = SecureHashTemplateParameters
            ),
            AttachmentConstraintParameters(ATTACHMENT_CONSTRAINT_TEMPLATE, "WhitelistedByZoneAttachmentConstraint"),
            AttachmentConstraintParameters(ATTACHMENT_CONSTRAINT_TEMPLATE, "AutomaticPlaceholderConstraint"),
            AttachmentConstraintParameters(ATTACHMENT_CONSTRAINT_TEMPLATE, "AutomaticHashConstraint")
        ) + PublicKeyTemplateParameters.all.map {
            AttachmentConstraintParameters(
                baseTemplateFile = ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_PUBLIC_KEY,
                implementationName = "SignatureAttachmentConstraint",
                pkTemplateParameters = it
            )
        }
    }
}
