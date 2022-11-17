package com.example.contract.token

import com.ing.zkflow.common.contracts.ZKContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName

/**
 * > Note the unfortunate hack: we have to implement both `Contract` and `ZKContract`, even though `ZKContract` already extends `Contract`.
 * > Otherwise Corda will not correctly detect your contract as a Corda Contract and therefore will not add its jar to the transaction
 * > as a ContractAttachment.
 *
 * All ZKFlow contracts should implement ZKContract. This ensure that checks are done on the publicly visible components of a transaction,
 * and not only on the private components.
 *
 * 'Public only' components are components that are not mentioned in any of the metadata of any command in this transaction.
 * That means they will not be checked by the ZKP circuits of this transaction.
 * 'Public Only' should therefore either not exist or have a check rule in this public verification function.
 * By default, ZKFlow enforces zero public only inputs and outputs for any ZKContract. If this is not acceptable,
 * please override this function and write appropriate checks.
 *
 */
class ExampleTokenContract : ZKContract, Contract {
    companion object {
        val ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }
}

