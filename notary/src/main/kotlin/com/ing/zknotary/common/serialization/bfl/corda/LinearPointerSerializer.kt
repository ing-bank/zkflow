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
    @FixedLength([MAX_CLASS_NAME_SIZE])
    val className: String,
    val isResolved: Boolean
) : Surrogate<LinearPointer<*>> {
    override fun toOriginal(): LinearPointer<*> {
        @Suppress("UNCHECKED_CAST")
        val klass = Class.forName(className) as Class<LinearState>
        return LinearPointer(pointer, klass, isResolved)
    }

    companion object {
        /**
         * The jvm specification does not place any limits on the size of a class name, the
         * implementation limits it to 65536. In practice however we don't expect to see class
         * names longer than the chosen limit.
         */
        const val MAX_CLASS_NAME_SIZE = 192
    }
}

object LinearPointerSerializer : KSerializer<LinearPointer<*>>
by (
    SurrogateSerializer(LinearPointerSurrogate.serializer()) {
        LinearPointerSurrogate(it.pointer, it.type.name, it.isResolved)
    }
    )
