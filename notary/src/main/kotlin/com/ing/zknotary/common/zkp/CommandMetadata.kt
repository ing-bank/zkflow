package com.ing.zknotary.common.zkp

import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SignatureScheme
import kotlin.reflect.KClass

@DslMarker
annotation class ZKCommandMetadataDSL

/**
 * This class describes the circuit associated  with this command.
 *
 * It containts information about locations, artifacts, etc., so that
 * ZKFLow knows how to use it.
 */
@ZKCommandMetadataDSL
data class ZKCircuit(
    /**
     * This name can be anything.
     * If  null, it will be derived from the command name.
     */
    var name: String? = null
)

/**
 * Describes the number of occurrences for a type.
 */
data class TypeCount(val type: KClass<out ContractState>, val count: Int)

@ZKCommandMetadataDSL
class TypeCountList : ArrayList<TypeCount>() {
    infix fun Int.of(type: KClass<out ContractState>) = add(TypeCount(type, this))
}

@ZKCommandMetadataDSL
class ZKCommandMetadata {
    /**
     * This is always true, and can't be changed
     */
    val networkParameters = true

    /**
     * The notary [SignatureScheme] type required by this circuit.
     *
     * This should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    var notarySignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    /**
     * The participant [SignatureScheme] type required by this circuit.
     *
     * Due to current limitations of the ZKP circuit, only one [SignatureScheme] per circuit is allowed for transaction participants.
     * This should be enforced at network level and therefore should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    var participantSignatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME

    /**
     * This determines whether a circuit is expected to exist for this command.
     *
     * If false, ZKFLow will ignore this command for the ZKP circuit in all ways, except for Merkle tree calculation.
     */
    var private = false

    /**
     * Infomation on the circuit and related artifacts to be used.
     *
     * If the command is marked private, but this is null, ZKFLow will
     * try to find the circuit based on some default rules. If that fails,
     * an error is thrown.
     */
    var circuit: ZKCircuit? = null

    var numberOfSigners = 0

    val inputs = TypeCountList()
    val references = TypeCountList()
    val outputs = TypeCountList()
    val userAttachments = TypeCountList()
    var timeWindow = false

    init {
        ZKFlow.requireSupportedSignatureScheme(participantSignatureScheme)
        ZKFlow.requireSupportedSignatureScheme(notarySignatureScheme)
    }

    fun circuit(init: ZKCircuit.() -> Unit): ZKCircuit {
        circuit = ZKCircuit().apply(init)
        return circuit!!
    }

    fun inputs(init: TypeCountList.() -> Unit) = inputs.apply(init)
    fun references(init: TypeCountList.() -> Unit) = references.apply(init)
    fun outputs(init: TypeCountList.() -> Unit) = outputs.apply(init)

    /**
     * These are only the attachments the user explicitly adds themselves.
     *
     * Contract attachments and other default attachments are added automatically
     * and calculated based on the number of contracts in the transaction.
     */
    fun userAttachments(init: TypeCountList.() -> Unit) = userAttachments.apply(init)

    /** Present when called, otherwise absent */
    fun timewindow() {
        timeWindow = true
    }
}

fun commandMetadata(init: ZKCommandMetadata.() -> Unit): ZKCommandMetadata {
    return ZKCommandMetadata().apply(init)
}
