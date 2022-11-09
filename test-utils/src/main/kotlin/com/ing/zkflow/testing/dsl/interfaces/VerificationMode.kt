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

/**
 * The different verification modes available for Zinc circuits.
 */
public enum class VerificationMode {
    /**
     * RUN mode indicates that, if the TransactionService supports it, it should
     * do a faster execution than a full setup/prove/verify cycle, but still uses the
     * actual circuit to verify the transaction.
     *
     * For example, in the case of Zinc, it can interpret an uncompiled circuit directly on
     * its VM and use it to prove and verify in one go, skipping the expensive setup and prove steps.
     */
    RUN,

    /**
     * Indicates that the TransactionService should use the full setup/prove/verify cycle,
     * in order to prove beyond any doubt that the transaction is valid in production settings.
     * This is expected to be very slow.
     */
    PROVE_AND_VERIFY,

    /**
     * Indicates that the TransactionService will use mock verification, by essentially only doing
     * the normal Corda transaction verification on the JVM.
     */
    MOCK
}
