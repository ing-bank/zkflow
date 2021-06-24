package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier

@Serializable
data class LinearPointerSurrogate(
    val pointer: @Contextual UniqueIdentifier,
    @FixedLength([CordaSerializers.CLASS_NAME_SIZE])
    val className: String,
    val isResolved: Boolean
) : Surrogate<LinearPointer<*>> {
    override fun toOriginal(): LinearPointer<*> {
        @Suppress("UNCHECKED_CAST")
        val klass = Class.forName(className) as Class<LinearState>
        return LinearPointer(pointer, klass, isResolved)
    }
}

object LinearPointerSerializer :
    SurrogateSerializer<LinearPointer<*>, LinearPointerSurrogate>(
        LinearPointerSurrogate.serializer(),
        { LinearPointerSurrogate(it.pointer, it.type.name, it.isResolved) }
    )
