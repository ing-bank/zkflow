package io.ivno.collateraltoken.serialization

import com.ing.zknotary.common.serialization.bfl.BFLSerializationScheme
import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import com.ing.zknotary.testing.dsl.zkLedger
import com.ing.zknotary.testing.serialization.serializeWithScheme
import io.ivno.collateraltoken.contract.ContractTest
import io.ivno.collateraltoken.contract.DepositContract
import io.ivno.collateraltoken.contract.IvnoTokenTypeContract
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DepositTransactionTransactionBuilderTest : ContractTest() {
    @Test
    fun `On deposit requesting, the transaction must include the Request command`() {
        services.zkLedger {
            zkTransaction {
                val memberships = createAllMemberships()
                reference(memberships.membershipFor(BANK_A).ref)
                reference(memberships.membershipFor(CUSTODIAN).ref)
                reference(memberships.membershipFor(TOKEN_ISSUING_ENTITY).ref)
                reference(memberships.attestationFor(BANK_A).ref)
                reference(memberships.attestationFor(CUSTODIAN).ref)
                reference(memberships.attestationFor(TOKEN_ISSUING_ENTITY).ref)
                reference(IvnoTokenTypeContract.ID, IVNO_TOKEN_TYPE)
                output(DepositContract.ID, DEPOSIT)
                fails()
                command(keysOf(BANK_A), DepositContract.Request)
                verifies()
            }
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `Deposit Request transaction created with ZKTransactionBuilder`() = withCustomSerializationEnv {
        val contractAttachmentId = services.attachments.getLatestContractAttachments(DepositContract.ID).first()
        val notary = services.networkParameters.notaries.first().identity
        val outputs = listOf(
            TransactionState(DEPOSIT, notary = notary, constraint = HashAttachmentConstraint(contractAttachmentId))
        )
        val commands = listOf(Command(DepositContract.Request, listOf(BANK_A.party.owningKey)))
        val attachments = listOf(contractAttachmentId)
        val timeWindow = TimeWindow.fromOnly(Instant.now())
        val references = emptyList<StateRef>()//List(2) { dummyStateRef() }

        val wtx = ZKTransactionBuilder(notary)
            .addOutputState(outputs.single())
            .addCommand(commands.single())
            .setTimeWindow(timeWindow)
            .toWireTransaction(services)
            .serialize().deserialize()

        // Doesn't verify because we have no reference states
        // We can't have reference states until we have implemented the ZK DSL
        // wtx.toLedgerTransaction(services).verify()

        /*
         * Confirm that the contents are actually serialized with BFL and not with something else.
         */
        val bflSerializedFirstOutput = outputs.first().serializeWithScheme(BFLSerializationScheme.SCHEME_ID)
        wtx.componentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }.components.first()
            .copyBytes() shouldBe bflSerializedFirstOutput.bytes

        wtx.outputs shouldBe outputs
        wtx.inputs shouldBe emptyList()
        wtx.commands.forEachIndexed { index, command ->
            command.value::class shouldBe commands[index].value::class
            command.signers shouldBe commands[index].signers

        }
        wtx.attachments shouldBe attachments
        wtx.notary shouldBe notary
        wtx.timeWindow shouldBe timeWindow
        wtx.references shouldBe references
    }

    private fun <R> withCustomSerializationEnv(block: () -> R): R {
        return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv { block() }
    }
}

