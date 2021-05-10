package io.ivno.collateraltoken.workflow

import io.dasl.contracts.v1.account.AccountState
import io.dasl.contracts.v1.crud.CrudCommands
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenContract
import io.dasl.workflows.api.flows.token.flows.MultiAccountTokenRedeemFlow
import io.dasl.workflows.api.flows.token.flows.MultiAccountTokenTransferFlow
import io.dasl.workflows.api.flows.token.flows.functions.TransferTokenSenderFunctions.prepareMultiTokenMoveWithSummary
import io.dasl.workflows.api.flows.token.flows.functions.TransferTokenSenderFunctions.prepareMultiTokenRedeemWithSummary
import io.dasl.workflows.api.flows.token.flows.functions.TransferTokenSenderFunctions.validateRequest
import io.ivno.collateraltoken.contract.*
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder

fun TransactionBuilder.addMembershipReferences(references: Iterable<ReferencedStateAndRef<*>>): TransactionBuilder {
    references.forEach { addReferenceState(it) }
    return this
}

fun TransactionBuilder.addMembershipReferences(vararg references: ReferencedStateAndRef<*>): TransactionBuilder {
    addMembershipReferences(references.toList())
    return this
}

fun TransactionBuilder.addIvnoTokenTypeCreation(state: IvnoTokenType): TransactionBuilder {
    addOutputState(state)
    addCommand(CrudCommands.Create, state.issuer.owningKey)
    return this
}

fun TransactionBuilder.addIvnoTokenTypeUpdate(
    oldState: StateAndRef<IvnoTokenType>,
    newState: IvnoTokenType
): TransactionBuilder {
    addInputState(oldState)
    addOutputState(newState)
    addCommand(CrudCommands.Update, newState.tokenIssuingEntity.owningKey)
    return this
}

fun TransactionBuilder.addDepositRequest(state: Deposit): TransactionBuilder {
    addOutputState(state)
    addCommand(DepositContract.Request, state.getRequiredSigningKeys())
    return this
}

fun TransactionBuilder.addDepositAdvance(oldState: StateAndRef<Deposit>, newState: Deposit): TransactionBuilder {
    addInputState(oldState)
    addOutputState(newState)
    addCommand(DepositContract.Advance, newState.getRequiredSigningKeys())
    return this
}

fun TransactionBuilder.addTransferRequest(transfer: Transfer): TransactionBuilder {
    addOutputState(transfer)
    addCommand(TransferContract.Request, transfer.getRequiredSigningKey())
    return this
}

fun TransactionBuilder.addTransferAdvance(oldState: StateAndRef<Transfer>, newState: Transfer): TransactionBuilder {
    addInputState(oldState)
    addOutputState(newState)
    addCommand(TransferContract.Advance, newState.getRequiredSigningKey())
    return this
}

fun TransactionBuilder.addRedemptionRequest(redemption: Redemption): TransactionBuilder {
    addOutputState(redemption)
    addCommand(RedemptionContract.Request, redemption.getRequiredSigningKeys())
    return this
}

fun TransactionBuilder.addRedemptionAdvance(
    oldState: StateAndRef<Redemption>,
    newState: Redemption
): TransactionBuilder {
    addInputState(oldState)
    addOutputState(newState)
    addCommand(RedemptionContract.Advance, newState.getRequiredSigningKeys())
    return this
}

fun TransactionBuilder.addIssuedToken(
    amount: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
    account: AccountState,
    tokenType: StateAndRef<IvnoTokenType>,
    holder: Party,
    custodian: Party,
    serviceHub: ServiceHub
): TransactionBuilder {
    val tokenState = TokenContract.generateIssuance(
        serviceHub,
        amount.quantity.toString(),
        tokenType,
        account.address
    )
    addOutputState(tokenState.withNewOwner(holder).ownableState, tokenState.contractId)
    addCommand(TokenContract.Command.Issue, custodian.owningKey)
    return this
}

fun TransactionBuilder.addMovedToken(
    request: MultiAccountTokenTransferFlow.Request,
    serviceHub: ServiceHub,
    ourIdentity: Party
): TransactionBuilder {
    validateRequest(request)
    prepareMultiTokenMoveWithSummary(request, serviceHub, ourIdentity)
    return this
}

fun TransactionBuilder.addRedeemToken(
    redemptionRequest: MultiAccountTokenRedeemFlow.Request,
    ourIdentity: Party,
    serviceHub: ServiceHub
): TransactionBuilder {
    prepareMultiTokenRedeemWithSummary(redemptionRequest, ourIdentity, serviceHub)
    return this
}
