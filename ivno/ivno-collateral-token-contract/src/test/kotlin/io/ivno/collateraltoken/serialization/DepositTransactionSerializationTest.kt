package io.ivno.collateraltoken.serialization

import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.crypto.zinc
import com.ing.zkflow.testing.dsl.VerificationMode
import com.ing.zkflow.testing.serialization.getSerializationContext
import com.ing.zkflow.testing.serialization.serializeWithScheme
import com.ing.zkflow.testing.withCustomSerializationEnv
import io.ivno.collateraltoken.contract.ContractTest
import io.ivno.collateraltoken.contract.DepositContract
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.random.Random


class DepositTransactionSerializationTest : ContractTest() {
    override val verificationMode: VerificationMode = VerificationMode.RUN
    override val commandData: CommandData = DepositContract.Request

    @Test
    @Suppress("LongMethod")
    fun `Deposit Request transaction serializes`() = withCustomSerializationEnv {
        val inputs = listOf(dummyStateRef())
        val constrainedOutputs = listOf(
            ConstrainedState(
                StateAndContract(DEPOSIT, DepositContract.ID),
                HashAttachmentConstraint(SecureHash.zeroHash)
            )
        )
        val commands = listOf(Command(commandData, listOf(BANK_A.party.owningKey)))
        val attachments = List(1) { SecureHash.randomSHA256() }
        val notary = TestIdentity.fresh("Notary").party
        val timeWindow = TimeWindow.fromOnly(Instant.now())
        val references = List(2) { dummyStateRef() }
        val networkParametersHash = services.networkParametersService.currentHash

        // This functionality is duplicated from ZKTransaction.toWireTransaction()
        val singleCommand = commands.singleOrNull() ?: error("Single command per transaction is allowed")
        val zkCommand =
            singleCommand.value as? ZKTransactionMetadataCommandData ?: error("Command must implement ZKTransactionMetadataCommandData")
        val additionalSerializationProperties =
            mapOf<Any, Any>(BFLSerializationScheme.CONTEXT_KEY_TRANSACTION_METADATA to zkCommand.transactionMetadata)

        val wtx = createWtx(
            inputs,
            constrainedOutputs,
            commands,
            attachments,
            notary,
            timeWindow,
            references,
            networkParametersHash,
            schemeId = BFLSerializationScheme.SCHEME_ID,
            additionalSerializationProperties = additionalSerializationProperties
        ).serialize().deserialize() // Deserialization must be forced, otherwise lazily mapped values will be picked up.

        /*
         * Confirm that the contents are actually serialized with BFL and not with something else.
         * This assertion becomes important once we start using the real ZKTransactionBuilder
         */
        val bflSerializedFirstInput = inputs.first().serializeWithScheme(BFLSerializationScheme.SCHEME_ID)
        wtx.componentGroups[ComponentGroupEnum.INPUTS_GROUP.ordinal].components.first()
            .copyBytes() shouldBe bflSerializedFirstInput.bytes

        wtx.outputs.zip(constrainedOutputs).forEach { (actual, expected) ->
            actual.data shouldBe expected.stateAndContract.state
            actual.contract shouldBe expected.stateAndContract.contract
            actual.notary shouldBe notary
            actual.constraint shouldBe expected.constraint
        }

        wtx.inputs shouldBe inputs
        wtx.commands.forEachIndexed { index, command ->
            command.value::class shouldBe commands[index].value::class
            command.signers shouldBe commands[index].signers

        }
        wtx.attachments shouldBe attachments
        wtx.notary shouldBe notary
        wtx.timeWindow shouldBe timeWindow
        wtx.references shouldBe references
        wtx.networkParametersHash shouldBe networkParametersHash
    }

    @Suppress("LongParameterList")
    private fun createWtx(
        inputs: List<StateRef> = emptyList(),
        outputs: List<ConstrainedState> = emptyList(),
        commands: List<Command<*>> = emptyList(),
        attachments: List<SecureHash> = emptyList(),
        notary: Party,
        timeWindow: TimeWindow = TimeWindow.fromOnly(Instant.now()),
        references: List<StateRef> = emptyList(),
        networkParametersHash: SecureHash = SecureHash.zeroHash,

        digestService: DigestService = DigestService.zinc,

        // The Id of the custom serializationScheme to use
        schemeId: Int,
        additionalSerializationProperties: Map<Any, Any> = emptyMap()
    ): WireTransaction {
        val serializationContext = getSerializationContext(schemeId, additionalSerializationProperties)

        return SerializationFactory.defaultFactory.withCurrentContext(serializationContext) {
            WireTransaction(
                createComponentGroups(
                    inputs,
                    outputs.map {
                        TransactionState(
                            data = it.stateAndContract.state,
                            it.stateAndContract.contract,
                            notary = notary,
                            constraint = it.constraint

                        )
                    },
                    commands,
                    attachments,
                    notary,
                    timeWindow,
                    references,
                    networkParametersHash
                ),
                PrivacySalt(),
                digestService
            )
        }
    }

    private data class ConstrainedState(val stateAndContract: StateAndContract, val constraint: AttachmentConstraint)

    private fun dummyStateRef() = StateRef(SecureHash.randomSHA256(), Random.nextInt())
}
