package io.ivno.collateraltoken.zinc.types

import com.ing.zkflow.zinc.types.optional
import io.onixlabs.corda.bnms.contract.Setting
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.Attestation
import io.onixlabs.corda.identityframework.contract.AttestationPointer
import io.onixlabs.corda.identityframework.contract.AttestationStatus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

fun Set<Setting<String>>.toJsonArray(size: Int, valueLength: Int): JsonArray = buildJsonArray {
    map { add(it.toJsonObject(valueLength).optional()) }
    addJsonArray {
        repeat(size-this@toJsonArray.size) {

        }
    }
}

fun Set<AbstractClaim<String>>.toJsonArray(size: Int, propertyLength: Int, valueLength: Int): JsonArray = buildJsonArray {
    map { add(it.toJsonObject(propertyLength, valueLength).optional()) }
    addJsonArray {
        repeat(size-this@toJsonArray.size) {

        }
    }
}

data class AttestationPointerBuilder<T: ContractState>(
    var stateRef: StateRef,
    var stateClass: Class<T>,
    var stateLinearId: UniqueIdentifier? = null
) {
    constructor(p: AttestationPointer<T>): this(p.stateRef, p.stateClass, p.stateLinearId)

    fun <S: ContractState> withStateClass(newStateClass: Class<S>) = AttestationPointerBuilder(
        stateRef, newStateClass, stateLinearId
    )

    fun build() = AttestationPointer(stateRef, stateClass, stateLinearId)
}

fun <T: ContractState> AttestationPointer<T>.builder() = AttestationPointerBuilder(this)

fun <T: ContractState> AttestationPointer<T>.copy(transform: AttestationPointerBuilder<T>.() -> Unit) =
    builder().apply(transform).build()

data class AttestationBuilder<T : ContractState>(
    var attestor: AbstractParty,
    var attestees: Set<AbstractParty>,
    var pointer: AttestationPointer<T>,
    var status: AttestationStatus,
    var metadata: Map<String, String>,
    var linearId: UniqueIdentifier,
    var previousStateRef: StateRef?
) {
    constructor(attestation: Attestation<T>) :
            this(
                attestation.attestor,
                attestation.attestees,
                attestation.pointer,
                attestation.status,
                attestation.metadata,
                attestation.linearId,
                attestation.previousStateRef
            )

    fun <S: ContractState> withPointer(newPointer: AttestationPointer<S>) = AttestationBuilder(
        attestor, attestees, newPointer, status, metadata, linearId, previousStateRef
    )

    fun build() = Attestation(
        attestor, attestees, pointer, status, metadata, linearId, previousStateRef
    )
}

fun <T: ContractState> Attestation<T>.builder() = AttestationBuilder(this)

fun <T: ContractState> Attestation<T>.copy(transform: AttestationBuilder<T>.() -> Unit) =
    builder().apply(transform).build()
