package io.ivno.annotated.fixtures

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.ZKP
import net.corda.core.contracts.UniqueIdentifier
import java.util.UUID

@ZKP
data class UniqueIdentifierSurrogate(
    val externalId: @ASCII(10) String?,
    val id: UUID
) : Surrogate<UniqueIdentifier> {
    override fun toOriginal() = UniqueIdentifier(externalId, id)
}

object UniqueIdentifierConverter : ConversionProvider<UniqueIdentifier, UniqueIdentifierSurrogate> {
    override fun from(original: UniqueIdentifier) =
        UniqueIdentifierSurrogate(original.externalId, original.id)
}
