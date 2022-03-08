@file:Suppress("FunctionName", "FunctionNaming", "FunctionParameterNaming", "LongParameterList", "TooManyFunctions") // Copy of Corda API

package com.ing.zkflow.testing.dsl.interfaces

import net.corda.core.DoNotImplement
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowException

/**
 * If you jumped here from a compiler error make sure the last line of your test tests for a transaction verify or fail.
 * This is a dummy type that can only be instantiated by functions in this module. This way we can ensure that all tests
 * will have as the last line either an accept or a failure test. The name is deliberately long to help make sense of
 * the triggered diagnostic.
 */
@DoNotImplement
public sealed class EnforceVerifyOrFail {
    internal object Token : EnforceVerifyOrFail()
}

public class DuplicateOutputLabel(label: String) : FlowException("Output label '$label' already used")
public class DoubleSpentInputs(ids: List<SecureHash>) :
    FlowException("Transactions spend the same input. Conflicting transactions ids: '$ids'")

public class AttachmentResolutionException(attachmentId: SecureHash) : FlowException("Attachment with id $attachmentId not found")
