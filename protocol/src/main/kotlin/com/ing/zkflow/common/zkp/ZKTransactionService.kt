package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

interface ZKTransactionService : SerializeAsToken {
    /**
     * this is used to get the logger for the caller of the caller.
     */
    private val loggerForMyCaller: Logger
        get() = LoggerFactory.getLogger(Throwable().stackTrace[2].className)

    fun setup(command: ZKTransactionMetadataCommandData, force: Boolean = false)
    fun setupTimed(
        command: ZKTransactionMetadataCommandData,
        force: Boolean = false,
        log: Logger = loggerForMyCaller
    ) {
        val time = measureTime {
            this.setup(command, force)
        }
        log.debug("[setup] $time")
    }

    fun prove(wtx: WireTransaction): ZKVerifierTransaction
    fun proveTimed(
        wtx: WireTransaction,
        log: Logger = loggerForMyCaller
    ): ZKVerifierTransaction {
        val timedValue = measureTimedValue {
            this.prove(wtx)
        }
        log.debug("[prove] ${timedValue.duration}")
        return timedValue.value
    }

    fun verify(svtx: SignedZKVerifierTransaction, checkSufficientSignatures: Boolean)
    fun verifyTimed(
        svtx: SignedZKVerifierTransaction,
        checkSufficientSignatures: Boolean,
        log: Logger = loggerForMyCaller
    ) {
        val time = measureTime {
            this.verify(svtx, checkSufficientSignatures)
        }
        log.debug("[prove] $time")
    }

    fun validateBackchain(tx: TraversableTransaction)
    fun zkServiceForTransactionMetadata(metadata: ResolvedZKTransactionMetadata): ZKService
}
