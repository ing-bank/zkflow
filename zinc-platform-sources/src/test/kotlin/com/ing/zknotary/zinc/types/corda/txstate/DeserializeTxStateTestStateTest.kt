package com.ing.zknotary.zinc.types.corda.txstate

import com.ing.zknotary.common.serialization.bfl.BFLSerializationScheme
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers.CLASS_NAME_SIZE
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.toBytes
import com.ing.zknotary.testing.bytesToWitness
import com.ing.zknotary.testing.fixtures.contract.TestContract
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.testing.zkp.ZKNulls
import com.ing.zknotary.zinc.types.emptyAnonymousParty
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.TransactionState
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializationMagic
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.serialization.serialize
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeTxStateTestStateTest {
    private val zincZKService = getZincZKService<DeserializeTxStateTestStateTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `a Tx state with TestState should be deserialized correctly`(txState: TransactionState<TestContract.TestState>) = withCustomSerializationEnv {
        val magic: SerializationMagic = CustomSerializationSchemeUtils
            .getCustomSerializationMagicFromSchemeId(BFLSerializationScheme.SCHEME_ID)
        val serializationContext = SerializationDefaults
            .P2P_CONTEXT
            .withPreferredSerializationVersion(magic)

        val witness = SerializationFactory.defaultFactory.withCurrentContext(serializationContext) {
            bytesToWitness(txState.serialize().bytes)
        }
        val expected = txState.toJsonObject().toString()

        zincZKService.run(witness, expected)

        // This has to be here, otherwise an Exception stating that
        // No tests are found will be thrown.
        assert(true)
    }

    private fun <R> withCustomSerializationEnv(block: () -> R): R {
        return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv { block() }
    }

    companion object {
        @JvmStatic
        fun testData() = listOf(
            StateAndContract(
                TestContract.TestState(ZKNulls.NULL_ANONYMOUS_PARTY, value = 88),
                TestContract.PROGRAM_ID
            )
        ).map {
            TransactionState(it.state, it.contract, ZKNulls.NULL_PARTY)
        }
    }
}

private fun TransactionState<TestContract.TestState>.toJsonObject() = buildJsonObject {
    // struct TxStateTestState {
    //         data: TestState,
    //         contract: String_256,
    //         notary: PartyEdDSA,
    //         encumbrance: NullableI32,
    //         constraint: AutomaticPlaceholderConstraint
    // }
    put("data", data.toJsonObject())
    put("contract", contract.toBytes().toJsonObject(CLASS_NAME_SIZE))
    put("notary", notary.toJsonObject(EdDSASurrogate.ENCODED_SIZE))
    put("encumbrance", encumbrance.toJsonObject())
    if (constraint is HashAttachmentConstraint || constraint is SignatureAttachmentConstraint) {
        put("constraint", constraint.toJsonObject())
    }
}

private fun TestContract.TestState.toJsonObject() = buildJsonObject {
    require(participants.size < 3) { "Max 2 participants per ${this@buildJsonObject::class.java}" }

    put("owner", owner.toJsonObject(EdDSASurrogate.ENCODED_SIZE))
    put("value", "$value")
    putJsonObject("participants") {
        put("size", "${participants.size}")
        putJsonArray("elements") {
            (
                participants.map { it.toJsonObject(EdDSASurrogate.ENCODED_SIZE) } +
                    List(2 - participants.size) { emptyAnonymousParty(EdDSASurrogate.ENCODED_SIZE) }
                ).forEach { add(it) }
        }
    }
}
