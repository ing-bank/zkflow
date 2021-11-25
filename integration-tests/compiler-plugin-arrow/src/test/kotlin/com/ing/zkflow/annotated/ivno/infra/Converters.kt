@file:Suppress("ClassName")

package com.ing.zkflow.annotated.ivno.infra

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.annotated.ivno.IvnoTokenType
import com.ing.zkflow.annotated.ivno.deps.BigDecimalAmount
import com.ing.zkflow.annotated.ivno.deps.Network
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.UUID

object EdDSAAnonymousPartyConverter : ConversionProvider<AnonymousParty, EdDSAAnonymousParty> {
    override fun from(original: AnonymousParty): EdDSAAnonymousParty {
        require(original.owningKey.algorithm == "EdDSA") {
            "This converter only accepts parties with EdDSA keys"
        }

        return EdDSAAnonymousParty(original.owningKey.encoded)
    }
}

object EdDSAPartyConverter : ConversionProvider<Party, EdDSAParty> {
    override fun from(original: Party): EdDSAParty {
        require(original.owningKey.algorithm == "EdDSA") {
            "This converter only accepts parties with EdDSA keys"
        }

        return EdDSAParty(
            original.name.toString(),
            original.owningKey.encoded
        )
    }
}

object NetworkAnonymousOperatorConverter : ConversionProvider<Network, NetworkEdDSAAnonymousOperator> {
    override fun from(original: Network): NetworkEdDSAAnonymousOperator {
        val operator = if (original.operator != null) {
            require(original.operator is AnonymousParty) { "Network must be managed by an anonymous party" }
            original.operator
        } else {
            null
        }?.let { EdDSAAnonymousPartyConverter.from(it) }

        return NetworkEdDSAAnonymousOperator(original.value, operator)
    }
}

object InstantConverter : ConversionProvider<Instant, InstantSurrogate> {
    override fun from(original: Instant) =
        InstantSurrogate(original.epochSecond, original.nano)
}

object UUIDConverter : ConversionProvider<UUID, UUIDSurrogate> {
    override fun from(original: UUID) =
        UUIDSurrogate(original.mostSignificantBits, original.leastSignificantBits)
}

object UniqueIdentifierConverter : ConversionProvider<UniqueIdentifier, UniqueIdentifierSurrogate> {
    override fun from(original: UniqueIdentifier) =
        UniqueIdentifierSurrogate(original.externalId, original.id)
}

object BigDecimalAmountConverter_LinearPointer_IvnoTokenType : ConversionProvider<
        BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType
        > {
    override fun from(original: BigDecimalAmount<LinearPointer<IvnoTokenType>>) =
        BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType(original.quantity, original.amountType)
}

object LinearPointerConverter_IvnoTokenType : ConversionProvider<
        LinearPointer<IvnoTokenType>,
        LinearPointerSurrogate_IvnoTokenType
        > {
    override fun from(original: LinearPointer<IvnoTokenType>) =
        LinearPointerSurrogate_IvnoTokenType(original.pointer, original.type.canonicalName, original.isResolved)
}
