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
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.utilities.ByteSequence
import net.corda.core.utilities.loggerFor
import zinc.transaction.serializer.CommandDataSerializerMap
import zinc.transaction.serializer.ContractStateSerializerMap
import java.nio.ByteBuffer
import java.security.PublicKey
import com.ing.serialization.bfl.api.deserialize as obliviousDeserialize
import com.ing.serialization.bfl.api.reified.deserialize as informedDeserialize
import com.ing.serialization.bfl.api.reified.serialize as informedSerialize
import com.ing.serialization.bfl.api.serialize as obliviousSerialize

open class TestBFLSerializationScheme : CustomSerializationScheme {
    companion object {
        const val SCHEME_ID = 602214076
    }

    override fun getSchemeId(): Int {
        return SCHEME_ID
    }

    private val logger = loggerFor<TestBFLSerializationScheme>()

    private val cordaSerdeMagicLength = CustomSerializationSchemeUtils.getCustomSerializationMagicFromSchemeId(SCHEME_ID).size

    private val serializersPublicKey = CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.DEFAULT_SIGNATURE_SCHEME)
    private val serializersModule = CordaSerializers + serializersPublicKey

    override fun <T : Any> deserialize(
        bytes: ByteSequence,
        clazz: Class<T>,
        context: SerializationSchemeContext
    ): T {
        logger.debug("Deserializing tx component:\t$clazz")

        val serializedData = bytes.bytes.drop(cordaSerdeMagicLength).toByteArray()

        // TODO is better matching possible?
        return when (clazz.canonicalName) {
            TransactionState::class.java.canonicalName -> {
                val (stateStrategy, message) = ContractStateSerializerMap.extractSerializerAndSerializedData(serializedData)

                @Suppress("UNCHECKED_CAST")
                informedDeserialize(message, TransactionStateSerializer(stateStrategy), serializersModule = serializersModule) as T
            }

            List::class.java.canonicalName -> {
                // This case will be triggered when commands will be reconstructed.
                // This involves deserialization of the SIGNERS_GROUP to reconstruct commands.

                /*
                 * Here we read the actual length of the serialized collection from the serialized data
                 * We use it to give the deserializer the information it requires (for now).
                 * Once the BFL adds the fixed length info and uses it for this purpose on deserialization under the hood,
                 * we can remove this argument
                 */
                val actualLength = ByteBuffer.wrap(serializedData.copyOfRange(0, Int.SIZE_BYTES)).int

                @Suppress("UNCHECKED_CAST")
                informedDeserialize<List<PublicKey>>(
                    serializedData,
                    serializersModule = serializersModule,
                    outerFixedLength = intArrayOf(actualLength)
                ) as T
            }

            CommandData::class.java.canonicalName -> {
                val (commandStrategy, message) = CommandDataSerializerMap.extractSerializerAndSerializedData(serializedData)

                @Suppress("UNCHECKED_CAST")
                obliviousDeserialize(
                    message,
                    CommandData::class.java,
                    commandStrategy,
                    serializersModule = serializersModule
                ) as T
            }

            else -> obliviousDeserialize(serializedData, clazz, serializersModule = serializersModule)
        }
    }

    override fun <T : Any> serialize(obj: T, context: SerializationSchemeContext): ByteSequence {
        logger.debug("Serializing tx component:\t${obj::class}")

        val serialization = when (obj) {
            is TransactionState<*> -> when (obj.data) {
                is DummyState -> {
                    @Suppress("UNCHECKED_CAST")
                    obj as TransactionState<DummyState>

                    ContractStateSerializerMap.prefixWithIdentifier(
                        DummyState::class,
                        informedSerialize(obj, TransactionStateSerializer(DummyState.serializer()), serializersModule = serializersModule)
                    )
                }
                else -> error("Do not know how to serialize ${obj.data::class.simpleName}")
            }
            is CommandData -> {
                CommandDataSerializerMap.prefixWithIdentifier(obj::class, obliviousSerialize(obj, serializersModule = serializersModule))
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
                /*
                 * This case will be triggered when SIGNERS_GROUP will be processed.
                 * Components of this group are lists of signers of commands.
                */
                @Suppress("UNCHECKED_CAST")
                val signers = obj as? List<PublicKey> ?: error("Signers: Expected List<PublicKey>, actual ${obj::class.simpleName}")

                // TODO: The outerFixedLength is hardcoded here. Should come from the SerializationSchemeContext
                val signersFixedLengthFromContext = 3
                informedSerialize(
                    signers,
                    serializersModule = serializersModule,
                    outerFixedLength = intArrayOf(signersFixedLengthFromContext)
                )
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
