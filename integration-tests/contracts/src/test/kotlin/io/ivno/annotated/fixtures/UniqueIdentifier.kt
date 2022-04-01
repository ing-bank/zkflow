package io.ivno.annotated.fixtures

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKPSurrogate
import net.corda.core.contracts.UniqueIdentifier
import java.util.UUID

@ZKPSurrogate(UniqueIdentifierConverter::class)
data class UniqueIdentifierSurrogate(
    val externalId: @UTF8(10) String?,
    val id: UUID
) : Surrogate<UniqueIdentifier> {
    override fun toOriginal() = UniqueIdentifier(externalId, id)
}

object UniqueIdentifierConverter : ConversionProvider<UniqueIdentifier, UniqueIdentifierSurrogate> {
    override fun from(original: UniqueIdentifier) =
        UniqueIdentifierSurrogate(original.externalId, original.id)
}
