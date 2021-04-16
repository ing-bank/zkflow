package zinc.transaction

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.TimeWindowSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.TransactionStateSerializer
import com.ing.zknotary.testing.fixtures.state.DummyState
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.serialization.CustomSerializationScheme
import net.corda.core.serialization.SerializationSchemeContext
import net.corda.core.utilities.ByteSequence
import zinc.transaction.envelope.Envelope
import java.security.PublicKey
import kotlin.reflect.KClass
import com.ing.serialization.bfl.api.deserialize as obliviousDeserialize
import com.ing.serialization.bfl.api.reified.deserialize as informedDeserialize
import com.ing.serialization.bfl.api.reified.serialize as informedSerialize
import com.ing.serialization.bfl.api.serialize as obliviousSerialize

open class TestBFLSerializationScheme : CustomSerializationScheme {
    companion object {
        const val SCHEME_ID = 602214076

        const val CORDA_SERDE_MAGIC_LENGTH = 7
    }

    override fun getSchemeId(): Int {
        return SCHEME_ID
    }

    private val serializersPublicKey = CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.DEFAULT_SIGNATURE_SCHEME)
    private val serializersModule = CordaSerializers + serializersPublicKey

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserialize(
        bytes: ByteSequence,
        clazz: Class<T>,
        context: SerializationSchemeContext
    ): T {
        println("\tDeserializing:\n\t\t$clazz\n")

        val redaction = bytes.bytes.drop(CORDA_SERDE_MAGIC_LENGTH).toByteArray()

        // TODO is better matching possible?
        return when (clazz.canonicalName) {
            TransactionState::class.java.canonicalName -> {
                val (stateStrategy, message) = Envelope.unwrapState(redaction)
                val strategy = TransactionStateSerializer(stateStrategy)

                informedDeserialize(message, strategy, serializersModule = serializersModule) as T
            }

            List::class.java.canonicalName -> {
                // This case will be triggered when commands will be reconstructed.
                // This involves deserialization of the SIGNERS_GROUP to reconstruct commands.
                val envelope: Envelope.Signers = informedDeserialize(redaction, serializersModule = serializersModule)
                envelope.signers as T
            }

            CommandData::class.java.canonicalName -> {
                val (commandStrategy, message) = Envelope.unwrapCommand(redaction)

                obliviousDeserialize(message, CommandData::class.java, commandStrategy, serializersModule = serializersModule) as T
            }

            else -> obliviousDeserialize(redaction, clazz, serializersModule = serializersModule)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> serialize(obj: T, context: SerializationSchemeContext): ByteSequence {
        println("\tSerializing:\n\t\t${obj::class}")
        val serialization = when (obj) {
            is TransactionState<*> -> when (obj.data) {
                is DummyState -> {
                    obj as TransactionState<DummyState>

                    val strategy = TransactionStateSerializer(DummyState.serializer())
                    val body = informedSerialize(obj, strategy, serializersModule = serializersModule)

                    Envelope.wrap(DummyState::class, body)
                }
                else -> error("Do not know how to serialize ${obj.data::class.simpleName}")
            }
            is CommandData -> {
                val klass = obj::class as KClass<out CommandData>

                val body = obliviousSerialize(obj, serializersModule = serializersModule)
                Envelope.wrap(klass, body)
            }

            is TimeWindow -> {
                // `TimeWindow` is a sealed class with private variant classes.
                // Although a serializer is registered for `TimeWindow`, it won't be picked up for top-level variants.
                // It is also impossible to register top-level serializers for variants because they are private and
                // thus it is impossible to access them to define appropriate serializers.
                // Therefore, we cast any variant as a TimeWindow.
                informedSerialize(obj, TimeWindowSerializer, serializersModule = serializersModule)
            }
            is List<*> -> {
                // This case will be triggered when
                // SIGNERS_GROUP
                // will be processed.
                // Components of this group are lists of signers of commands.
                obj as? List<PublicKey> ?: error("Signers: Expected List<PublicKey>, actual ${obj::class.simpleName}")

                informedSerialize(Envelope.Signers(obj), serializersModule = serializersModule)
            }
            else -> {
                obliviousSerialize(obj, serializersModule = serializersModule)
            }
        }

        if (serialization.isEmpty()) {
            println("Conversion of serialization from ByteArray to ByteSequence will fail, because the latter require a non-empty byte array")
        }

        return ByteSequence.of(serialization)
    }
}
