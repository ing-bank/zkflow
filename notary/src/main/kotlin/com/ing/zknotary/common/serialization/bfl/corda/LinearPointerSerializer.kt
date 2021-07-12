package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers.CLASS_NAME_SIZE
import com.ing.zknotary.common.serialization.bfl.serializers.getOriginalClass
import com.ing.zknotary.common.serialization.bfl.serializers.toBytes
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier

@Serializable
@Suppress("ArrayInDataClass")
data class LinearPointerSurrogate(
    val pointer: @Contextual UniqueIdentifier,
    @FixedLength([CLASS_NAME_SIZE])
    val className: ByteArray,
    val isResolved: Boolean
) : Surrogate<LinearPointer<*>> {
    override fun toOriginal(): LinearPointer<*> {
        @Suppress("UNCHECKED_CAST")
        val klass = className.getOriginalClass() as Class<LinearState>
        return LinearPointer(pointer, klass, isResolved)
    }
}

object LinearPointerSerializer :
    SurrogateSerializer<LinearPointer<*>, LinearPointerSurrogate>(
        LinearPointerSurrogate.serializer(),
        { LinearPointerSurrogate(it.pointer, it.type.toBytes(), it.isResolved) }
    )
