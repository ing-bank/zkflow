package io.ivno.collateraltoken.workflow

import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

internal object INITIALIZING : Step("Initializing flow.")

internal object GENERATING : Step("Generating transaction.")

internal object VERIFYING : Step("Verifying transaction.")

internal object SIGNING : Step("Signing transaction.") {
    override fun childProgressTracker(): ProgressTracker = SignTransactionFlow.tracker()
}

internal object COUNTERSIGNING : Step("Collecting counter-party signatures.") {
    override fun childProgressTracker(): ProgressTracker = CollectSignaturesFlow.tracker()
}

internal object FINALIZING : Step("Finalizing transaction.") {
    override fun childProgressTracker(): ProgressTracker = FinalityFlow.tracker()
}

internal object RECORDING : Step("Recording finalized transaction.")

internal object SYNCHRONIZING : Step("Synchronizing membership.")
