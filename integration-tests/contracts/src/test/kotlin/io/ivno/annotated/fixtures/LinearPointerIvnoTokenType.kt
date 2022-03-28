@file:Suppress("ClassName")

package io.ivno.annotated.fixtures

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKPSurrogate
import io.ivno.annotated.IvnoTokenType
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier

@ZKPSurrogate(LinearPointerConverter_IvnoTokenType::class)
data class LinearPointerSurrogate_IvnoTokenType(
    val pointer: @Via<UniqueIdentifierSurrogate> UniqueIdentifier,
    val className: @Size(100) String,
    val isResolved: Boolean
) : Surrogate<LinearPointer<IvnoTokenType>> {
    override fun toOriginal(): LinearPointer<IvnoTokenType> {
        @Suppress("UNCHECKED_CAST")
        val klass = Class.forName(className) as Class<IvnoTokenType>
        return LinearPointer(pointer, klass, isResolved)
    }
}

object LinearPointerConverter_IvnoTokenType : ConversionProvider<
        LinearPointer<IvnoTokenType>,
        LinearPointerSurrogate_IvnoTokenType
        > {
    override fun from(original: LinearPointer<IvnoTokenType>) =
        LinearPointerSurrogate_IvnoTokenType(original.pointer, original.type.canonicalName, original.isResolved)
}
