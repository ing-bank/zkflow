package io.ivno.collateraltoken.workflow

import net.corda.core.contracts.ContractState
import net.corda.core.transactions.SignedTransaction

inline fun <reified T : ContractState> SignedTransaction.singleOutRefOfType() = tx.outRefsOfType<T>().single()

inline fun <reified T : ContractState> SignedTransaction.singleOutputOfType() = tx.outputsOfType<T>().single()
