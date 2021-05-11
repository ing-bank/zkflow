package io.ivno.collateraltoken.serialization

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.serialization.bfl.BFLSerializationScheme
import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.testing.serialization.getSerializationContext
import com.ing.zknotary.testing.serialization.serializeWithScheme
import io.ivno.collateraltoken.contract.ContractTest
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.DepositContract
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.*
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.random.Random
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DepositTransactionSerializationTest : ContractTest() {
    @Test
    @Suppress("LongMethod")
    fun `Deposit Request transaction serializes`() = withCustomSerializationEnv {
        ContractStateSerializerMap.register(Deposit::class, 1, Deposit.serializer())
        CommandDataSerializerMap.register(DepositContract.Request::class, 3, DepositContract.Request.serializer())

        val inputs = listOf(dummyStateRef()) // TODO: This should be remove once the outputs are added back
        val constrainedOutputs = listOf(
            ConstrainedState(
                StateAndContract(DEPOSIT, DepositContract.ID),
                HashAttachmentConstraint(SecureHash.zeroHash)
            )
        )
        val commands = listOf(Command(DepositContract.Request, listOf(BANK_A.party.owningKey)))
        val attachments = List(1) { SecureHash.randomSHA256() }
        val notary = TestIdentity.fresh("Notary").party
        val timeWindow = TimeWindow.fromOnly(Instant.now())
        val references = List(2) { dummyStateRef() }
        val networkParametersHash = SecureHash.randomSHA256()

        // This functionality is duplicated from ZKTransaction.toWireTransaction()
        val command = commands.singleOrNull() ?: error("Single command per transaction is allowed")
        val zkCommand = command.value as? ZKCommandData ?: error("Command must implement ZKCommandData")
        val additionalSerializationProperties =
            mapOf<Any, Any>(BFLSerializationScheme.CONTEXT_KEY_CIRCUIT to zkCommand.circuit)

        val wtx = createWtx(
            inputs,
            emptyList(), // TODO: create missing serializers for constrainedOutputs,
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
        wtx.commands shouldBe commands
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

    private fun <R> withCustomSerializationEnv(block: () -> R): R {
        return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv { block() }
    }

    private data class ConstrainedState(val stateAndContract: StateAndContract, val constraint: AttachmentConstraint)

    private fun dummyStateRef() = StateRef(SecureHash.randomSHA256(), Random.nextInt())
}
