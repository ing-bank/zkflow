package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.zkp.ZKFlow
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SignatureScheme
import java.io.File
import java.time.Duration
import java.util.function.Predicate
import kotlin.reflect.KClass

data class ResolvedZKCircuit(
    val commandKClass: KClass<out CommandData>,
    var name: String,
    /**
     * Unless provided, this will be calculated to be `<gradle module>/src/main/zinc/<transaction.name>/commands/<command.name>`
     * This is where the circuit elements for this command can be found
     */
    val buildFolder: File,
    val javaClass2ZincType: Map<KClass<out ContractState>, ZincType>,
    val buildTimeout: Duration,
    val setupTimeout: Duration,
    val provingTimeout: Duration,
    val verificationTimeout: Duration
)

internal interface ResolvedCommandMetadata {
    val commandKClass: KClass<out CommandData>
    val commandSimpleName: String

    /**
     * The notary [SignatureScheme] type required by this command.
     *
     * This should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    val notarySignatureScheme: SignatureScheme

    /**
     * The participant [SignatureScheme] type required by this command.
     *
     * Due to current limitations of the ZKP command, only one [SignatureScheme] per command is allowed for transaction participants.
     * This should be enforced at network level and therefore should match the [SignatureScheme] defined for the network notary
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    val participantSignatureScheme: SignatureScheme

    /**
     * The attachment constraint required by this command for all states
     *
     * Due to current limitations of the ZKP command, only one [AttachmentConstraint] per transaction is allowed.
     * This should be enforced at network level and therefore should match the [AttachmentConstraint] defined for the network
     * in the transaction metadata. If they don't match, an error is thrown.
     */
    val attachmentConstraintType: KClass<out AttachmentConstraint>

    val numberOfSigners: Int

    val inputs: List<ContractStateTypeCount>
    val references: List<ContractStateTypeCount>
    val outputs: List<ContractStateTypeCount>
    val numberOfUserAttachments: Int
    val timeWindow: Boolean

    /**
     * This is always true, and can't be changed
     */
    val networkParameters: Boolean

    /**
     * The list of all contract Class names used by all states in this command
     */
    val contractClassNames: List<ContractClassName>
}

abstract class ResolvedZKCommandMetadata(
    final override val notarySignatureScheme: SignatureScheme,
    final override val participantSignatureScheme: SignatureScheme,
    final override val attachmentConstraintType: KClass<out AttachmentConstraint>
) : ResolvedCommandMetadata {
    override val networkParameters = true
    override val commandSimpleName: String by lazy { commandKClass.simpleName ?: error("Command classes must be a named class") }
    override val contractClassNames: List<ContractClassName>
        get() {
            val stateTypes = (inputs.expanded + outputs.expanded + references.expanded).distinct()
            return stateTypes.map {
                requireNotNull(it.requiredContractClassName) {
                    "Unable to infer Contract class name because state class $it is not annotated with " +
                        "@BelongsToContract, and does not have an enclosing class which implements Contract."
                }
            }
        }

    init {
        ZKFlow.requireSupportedSignatureScheme(participantSignatureScheme)
        ZKFlow.requireSupportedSignatureScheme(notarySignatureScheme)
    }
}

@Suppress("LongParameterList") // param length caused by Corda component count
class PrivateResolvedZKCommandMetadata(
    /**
     * Information on the circuit and related artifacts to be used.
     */
    val circuit: ResolvedZKCircuit,
    /**
     * Defines which transaction components should be "hidden" with ZKP
     */
    val zkFiltering: Predicate<Any>,
    override val commandKClass: KClass<out CommandData>,
    override val numberOfSigners: Int,
    override val inputs: List<ContractStateTypeCount>,
    override val references: List<ContractStateTypeCount>,
    override val outputs: List<ContractStateTypeCount>,
    override val numberOfUserAttachments: Int,
    override val timeWindow: Boolean,
    notarySignatureScheme: SignatureScheme,
    participantSignatureScheme: SignatureScheme,
    attachmentConstraintType: KClass<out AttachmentConstraint>,
) : ResolvedZKCommandMetadata(notarySignatureScheme, participantSignatureScheme, attachmentConstraintType)

@Suppress("LongParameterList") // param length caused by Corda component count
class PublicResolvedZKCommandMetadata(
    override val commandKClass: KClass<out CommandData>,
    override val numberOfSigners: Int,
    override val inputs: List<ContractStateTypeCount>,
    override val references: List<ContractStateTypeCount>,
    override val outputs: List<ContractStateTypeCount>,
    override val numberOfUserAttachments: Int,
    override val timeWindow: Boolean,
    notarySignatureScheme: SignatureScheme,
    participantSignatureScheme: SignatureScheme,
    attachmentConstraintType: KClass<out AttachmentConstraint>,
) : ResolvedZKCommandMetadata(notarySignatureScheme, participantSignatureScheme, attachmentConstraintType)
