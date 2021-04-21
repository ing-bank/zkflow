package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier

@Serializable
data class LinearPointerSurrogate(
    val pointer: @Contextual UniqueIdentifier,
    @FixedLength([128])
    val className: String,
    val isResolved: Boolean
) : Surrogate<LinearPointer<*>> {
    override fun toOriginal(): LinearPointer<*> {
        return LinearPointer(pointer, Class.forName(className) as Class<LinearState>, isResolved)
    }
}

object LinearPointerSerializer : KSerializer<LinearPointer<*>>
    by (
        SurrogateSerializer(LinearPointerSurrogate.serializer()) {
            LinearPointerSurrogate(it.pointer, it.type.name, it.isResolved)
        }
        )
