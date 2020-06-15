package com.ing.zknotary.common.transactions

import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.internal.internalFindTrustedAttachmentForClass
import net.corda.core.internal.log
import net.corda.core.internal.validConstraints
import net.corda.core.internal.warnOnce
import net.corda.core.node.ServicesForResolution
import net.corda.core.node.services.AttachmentId
import net.corda.core.utilities.contextLogger
import java.util.regex.Pattern

/*
    All taken from ContraintsUtils, because they are private or internal there.
 */
/**
 * Fails if the constraint is not of a known type.
 * Only the Corda core is allowed to implement the [AttachmentConstraint] interface.
 */
fun checkConstraintValidity(state: TransactionState<*>) {
    val validConstraints = setOf(
        AlwaysAcceptAttachmentConstraint::class,
        HashAttachmentConstraint::class,
        WhitelistedByZoneAttachmentConstraint::class,
        SignatureAttachmentConstraint::class
    )

    require(state.constraint::class in validConstraints) { "Found state ${state.contract} with an illegal constraint: ${state.constraint}" }
    if (state.constraint is AlwaysAcceptAttachmentConstraint) {
        log.warnOnce("Found state ${state.contract} that is constrained by the insecure: AlwaysAcceptAttachmentConstraint.")
    }
}


class ZKTransactionBuilderHelper() {
    private companion object {
        private val log = contextLogger()
        private val MISSING_CLASS_DISABLED = java.lang.Boolean.getBoolean("net.corda.transactionbuilder.missingclass.disabled")

        private const val ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"
        private val FQCP: Pattern = Pattern.compile("$ID_PATTERN(/$ID_PATTERN)+")
        private fun isValidJavaClass(identifier: String) = FQCP.matcher(identifier).matches()
        private fun Collection<*>.deepEquals(other: Collection<*>): Boolean {
            return (size == other.size) && containsAll(other) && other.containsAll(this)
        }
        private fun Collection<AttachmentId>.toPrettyString(): String = sorted().joinToString(
            separator = System.lineSeparator(),
            prefix = System.lineSeparator()
        )
    }


    private fun addMissingAttachment(missingClass: String, services: ServicesForResolution, originalException: Throwable): Boolean {
        if (!ZKTransactionBuilderHelper.isValidJavaClass(missingClass)) {
            ZKTransactionBuilderHelper.log.warn("Could not autodetect a valid attachment for the transaction being built.")
            throw originalException
        } else if (ZKTransactionBuilderHelper.MISSING_CLASS_DISABLED) {
            ZKTransactionBuilderHelper.log.warn("BROKEN TRANSACTION, BUT AUTOMATIC DETECTION OF {} IS DISABLED!", missingClass)
            throw originalException
        }

        val attachment = services.attachments.internalFindTrustedAttachmentForClass(missingClass)

        if (attachment == null) {
            ZKTransactionBuilderHelper.log.error("""The transaction currently built is missing an attachment for class: $missingClass.
                        Attempted to find a suitable attachment but could not find any in the storage.
                        Please contact the developer of the CorDapp for further instructions.
                    """.trimIndent())
            throw originalException
        }

        ZKTransactionBuilderHelper.log.warnOnce("""The transaction currently built is missing an attachment for class: $missingClass.
                        Automatically attaching contract dependency $attachment.
                        Please contact the developer of the CorDapp and install the latest version, as this approach might be insecure.
                    """.trimIndent())

        addAttachment(attachment.id)
        return true
    }


}
