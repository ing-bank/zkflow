package zinc.transaction

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.TimeWindowSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.TransactionStateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSAPublicKeySerializer
import com.ing.zknotary.testing.fixtures.state.DummyState
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.serialization.CustomSerializationScheme
import net.corda.core.serialization.SerializationSchemeContext
import net.corda.core.utilities.ByteSequence
import net.i2p.crypto.eddsa.EdDSAPublicKey
import com.ing.serialization.bfl.api.reified.serialize as informedSerialize
import com.ing.serialization.bfl.api.serialize as obliviousSerialize

open class TestBFLSerializationScheme : CustomSerializationScheme {
    companion object {
        const val SCHEME_ID = 602214076
    }

    override fun getSchemeId(): Int {
        return SCHEME_ID
    }

    private val serializersPublicKey = CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.DEFAULT_SIGNATURE_SCHEME)
    private val serializersModule = CordaSerializers + serializersPublicKey

    override fun <T : Any> deserialize(
        bytes: ByteSequence,
        clazz: Class<T>,
        context: SerializationSchemeContext
    ): T {
        TODO("Deserializer says Sowwy")
        // return SerializationFactory.defaultFactory.deserialize(bytes, clazz, SerializationDefaults.P2P_CONTEXT)
    }

    override fun <T : Any> serialize(obj: T, context: SerializationSchemeContext): ByteSequence {
        println("\t${obj::class}")
        val serialization = when (obj) {
            is TransactionState<*> -> when (obj.data) {
                is DummyState -> {
                    @Suppress("UNCHECKED_CAST")
                    obj as TransactionState<DummyState>
                    val strategy = TransactionStateSerializer(DummyState.serializer())
                    informedSerialize(obj, strategy, serializersModule = serializersModule)
                }
                else -> error("Do not know how to serialize ${obj.data::class.simpleName}")
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
                // This case will be triggered when SIGNERS_GROUP will be processed.
                // Components of SIGNERS_GROUP are lists of signers of commands.

                // To be able to serialize Lists, they must be casted explicitly.
                // This is due to a bug (?) in kotlinx.serialization.
                // There must be at least one element of the list to know the actual inner type and
                // thus to select the right strategy.
                // FYI: `obj::class.typeParameters.single()` -> `E`
                val element = obj.first() ?: error("List must contain at least one element")

                when (element) {
                    is EdDSAPublicKey -> {
                        obj as List<EdDSAPublicKey>
                        val strategy = ListSerializer(EdDSAPublicKeySerializer)
                        informedSerialize(
                            obj,
                            strategy = strategy,
                            serializersModule = serializersModule,
                            outerFixedLength = intArrayOf(obj.size)
                        )
                    }
                    else -> error("Cannot select serializer of inner type")
                }
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
