/*
 * Source attribution:
 *
 * The classes for the ZKFlow test DSL are strongly based on their original non-ZKP counterpart from Corda
 * itself, as defined in the package net.corda.testing.dsl (https://github.com/corda/corda).
 *
 * Ideally ZKFlow could have extended the Corda test DSL to add the ZKP-related parts only, and leave the rest of the behaviour intact.
 * Unfortunately, Corda's test DSL is hard to extend, and it was not possible to add this behaviour without copying most
 * of the original.
 */

package com.ing.zkflow.testing.dsl.interfaces

import net.corda.core.DoNotImplement

/**
 * This interface asserts that the DSL at hand is capable of verifying its underlying construct(ledger/transaction).
 */
@DoNotImplement
public interface Verifies {
    /**
     * Verifies the ledger/transaction, throws if the verification fails.
     * @param mode The [VerificationMode] to use for verification.
     */
    public fun verifies(): EnforceVerifyOrFail

    /**
     * Asserts that verifies() throws.
     * @param expectedMessage An optional string to be searched for in the raised exception.
     */
    public fun failsWith(expectedMessage: String?): EnforceVerifyOrFail {
        val exceptionThrown = try {
            verifies()
            false
        } catch (exception: Exception) {
            if (expectedMessage != null) {
                val exceptionMessage = exception.message
                if (exceptionMessage == null) {
                    throw AssertionError(
                        "Expected exception containing '$expectedMessage' but raised exception had no message",
                        exception
                    )
                } else if (!exceptionMessage.toLowerCase().contains(expectedMessage.toLowerCase())) {
                    throw AssertionError(
                        "Expected exception containing '$expectedMessage' but raised exception was '$exception'",
                        exception
                    )
                }
            }
            true
        }

        if (!exceptionThrown) {
            throw AssertionError("Expected exception but didn't get one")
        }

        return EnforceVerifyOrFail.Token
    }

    /**
     * Asserts that [verifies] throws, with no condition on the exception message.
     */
    public fun fails(): EnforceVerifyOrFail = failsWith(null)

    /**
     * @see failsWith
     */
    public infix fun `fails with`(msg: String): EnforceVerifyOrFail = failsWith(msg)
}
