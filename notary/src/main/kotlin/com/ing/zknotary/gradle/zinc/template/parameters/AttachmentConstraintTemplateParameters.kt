package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class AttachmentConstraintTemplateParameters(
    val baseTemplateFile: String,
    val implementationName: String,
    val secureHashParams: SecureHashTemplateParameters? = null,
    val polyPublicKeyParams: PolyTemplateParameters<PublicKeyTemplateParameters>? = null
) : TemplateParameters(
    baseTemplateFile,
    listOfNotNull(
        secureHashParams,
        polyPublicKeyParams
    )
) {
    // Convenience constructor, to simplify configurations in the gradle plugin
    constructor(
        baseTemplateFile: String,
        implementationName: String,
        secureHashParams: SecureHashTemplateParameters? = null,
        publicKeyParams: PublicKeyTemplateParameters?
    ) : this(
        baseTemplateFile, implementationName, secureHashParams,
        publicKeyParams?.let { PolyTemplateParameters(it) }
    )

    private val algName by lazy { polyPublicKeyParams?.type?.algName ?: "" }
    override val typeName by lazy { "$implementationName$algName" }

    override fun getReplacements() = getTypeReplacements() +
        (
            polyPublicKeyParams
                ?.getTypeReplacements("PK_")
                ?: emptyMap()
            )

    override fun getTargetFilename() = getFileName()

    companion object {
        private const val ATTACHMENT_CONSTRAINT_TEMPLATE = "attachment_constraint.zn"
        private const val ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_SECURE_HASH = "attachment_constraint_with_secure_hash.zn"
        private const val ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_PUBLIC_KEY = "attachment_constraint_with_public_key.zn"

        val all = listOf(
            AttachmentConstraintTemplateParameters(ATTACHMENT_CONSTRAINT_TEMPLATE, "AlwaysAcceptAttachmentConstraint"),
            AttachmentConstraintTemplateParameters(
                baseTemplateFile = ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_SECURE_HASH,
                implementationName = "HashAttachmentConstraint",
                secureHashParams = SecureHashTemplateParameters
            ),
            AttachmentConstraintTemplateParameters(ATTACHMENT_CONSTRAINT_TEMPLATE, "WhitelistedByZoneAttachmentConstraint"),
            AttachmentConstraintTemplateParameters(ATTACHMENT_CONSTRAINT_TEMPLATE, "AutomaticPlaceholderConstraint"),
            AttachmentConstraintTemplateParameters(ATTACHMENT_CONSTRAINT_TEMPLATE, "AutomaticHashConstraint")
        ) + PublicKeyTemplateParameters.all.map {
            AttachmentConstraintTemplateParameters(
                baseTemplateFile = ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_PUBLIC_KEY,
                implementationName = "SignatureAttachmentConstraint",
                publicKeyParams = it
            )
        }
        val polymorphic = all.map {
            PolyTemplateParameters(it)
        }
    }
}
