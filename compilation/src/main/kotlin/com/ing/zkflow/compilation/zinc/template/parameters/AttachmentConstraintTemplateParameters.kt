package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters

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
        ) + PublicKeyTemplateParameters.all.map {
            AttachmentConstraintTemplateParameters(
                baseTemplateFile = ATTACHMENT_CONSTRAINT_TEMPLATE_WITH_PUBLIC_KEY,
                implementationName = "SignatureAttachmentConstraint",
                publicKeyParams = it
            )
        }

        // TODO: this fails for SignatureAttachmentConstraint: there will be multiple matches.
        // This bug slipped through the cracks because all tests use HashAttachmentConstraint
        // Fix later, or not relevant because all this code will go away with ZincPoet
        fun selectParameters(attachmentQualifiedClassName: String) =
            all.single { it.implementationName == attachmentQualifiedClassName.split(".").last() }

        val polymorphic = all.map { it.polymorphic() }
    }
}
