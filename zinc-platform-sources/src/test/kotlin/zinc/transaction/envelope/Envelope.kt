package zinc.transaction.envelope

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import java.nio.ByteBuffer
import java.security.PublicKey
import kotlin.reflect.KClass

object Envelope {
    private val state2Id = mutableMapOf<KClass<out ContractState>, Int>()
    private val stateId2strategy = mutableMapOf<Int, KSerializer<out ContractState>>()

    private val command2Id = mutableMapOf<KClass<out CommandData>, Int>()
    private val commandId2strategy = mutableMapOf<Int, KSerializer<out CommandData>>()

    @Serializable
    data class Signers(@FixedLength([2]) val signers: List<PublicKey>)

    @JvmName("registerState")
    fun <T : ContractState> register(klass: KClass<out T>, id: Int, strategy: KSerializer<out T>) {
        state2Id.put(klass, id)?.let { throw EnvelopeError.StateRegistration(klass) }
        stateId2strategy.put(id, strategy)?.let { throw EnvelopeError.StateRegistration(klass) }
    }

    @JvmName("registerCommand")
    fun <T : CommandData> register(klass: KClass<out T>, id: Int, strategy: KSerializer<out T>) {
        command2Id.put(klass, id)?.let { throw EnvelopeError.CommandRegistration(klass) }
        commandId2strategy.put(id, strategy)?.let { throw EnvelopeError.CommandRegistration(klass) }
    }

    @JvmName("wrapState")
    fun wrap(klass: KClass<out ContractState>, body: ByteArray): ByteArray {
        val id = state2Id[klass] ?: throw EnvelopeError.StateNoRegistration(klass)
        return ByteBuffer.allocate(4).putInt(id).array() + body
    }

    fun unwrapState(message: ByteArray): Pair<KSerializer<out ContractState>, ByteArray> {
        val stamp = ByteBuffer.wrap(message.copyOfRange(0, 4)).int
        val body = message.drop(4).toByteArray()
        val strategy = stateId2strategy[stamp] ?: throw EnvelopeError.StateNoRegistration(stamp)

        return Pair(strategy, body)
    }

    @JvmName("wrapCommand")
    fun wrap(klass: KClass<out CommandData>, body: ByteArray): ByteArray {
        val id = command2Id[klass] ?: throw EnvelopeError.CommandNoRegistration(klass)
        return ByteBuffer.allocate(4).putInt(id).array() + body
    }

    fun unwrapCommand(message: ByteArray): Pair<KSerializer<out CommandData>, ByteArray> {
        val stamp = ByteBuffer.wrap(message.copyOfRange(0, 4)).int
        val body = message.drop(4).toByteArray()
        val strategy = commandId2strategy[stamp] ?: throw EnvelopeError.CommandNoRegistration(stamp)

        return Pair(strategy, body)
    }
}
