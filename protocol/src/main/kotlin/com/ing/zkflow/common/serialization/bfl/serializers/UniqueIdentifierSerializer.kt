package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.UniqueIdentifier
import java.util.UUID

object UniqueIdentifierSerializer :
    SurrogateSerializer<UniqueIdentifier, UniqueIdentifierSurrogate>(
        UniqueIdentifierSurrogate.serializer(),
        { UniqueIdentifierSurrogate(it.externalId, it.id) }
    )

@Serializable
data class UniqueIdentifierSurrogate(
    @FixedLength([EXTERNAL_ID_LENGTH])
    val externalId: String?,
    val id: @Contextual UUID
) : Surrogate<UniqueIdentifier> {
    override fun toOriginal(): UniqueIdentifier = UniqueIdentifier(externalId, id)

    companion object {
        // externalId denotes any existing weak identifier acting as a human readable identity paired with the strong UUID,
        // and as such there is no specific information on its maximum length, leading to an arbitrary choice being made
        // that nevertheless seems capable to support the functionality of this externalId
        const val EXTERNAL_ID_LENGTH = 50
    }
}
