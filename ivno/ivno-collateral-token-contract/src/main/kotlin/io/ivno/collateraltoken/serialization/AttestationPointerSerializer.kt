package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.getOriginalClass
import com.ing.zknotary.common.serialization.bfl.serializers.toBytes
import io.onixlabs.corda.identityframework.contract.AttestationPointer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier

@Serializable
@Suppress("ArrayInDataClass")
data class AttestationPointerSurrogate(
    val stateRef: @Contextual StateRef,
    @FixedLength([MAX_CLASS_NAME_SIZE])
    val stateClassName: ByteArray,
    val stateLinearId: @Contextual UniqueIdentifier?,
) : Surrogate<AttestationPointer<*>> {
    override fun toOriginal() : AttestationPointer<*> {
        @Suppress("UNCHECKED_CAST")
        val klass = stateClassName.getOriginalClass() as Class<ContractState>
        return AttestationPointer(stateRef, klass, stateLinearId)
    }

    companion object {
        const val MAX_CLASS_NAME_SIZE = CordaSerializers.CLASS_NAME_SIZE
    }
}

object AttestationPointerSerializer :
    SurrogateSerializer<AttestationPointer<*>, AttestationPointerSurrogate>(
        AttestationPointerSurrogate.serializer(),
        { AttestationPointerSurrogate(it.stateRef, it.stateClass.toBytes(), it.stateLinearId) }
    )
