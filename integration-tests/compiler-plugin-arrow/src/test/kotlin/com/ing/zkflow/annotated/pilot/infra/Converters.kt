@file:Suppress("ClassName")

package com.ing.zkflow.annotated.pilot.infra

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.annotated.pilot.ivno.IvnoTokenType
import com.ing.zkflow.annotated.pilot.ivno.deps.BigDecimalAmount
import com.ing.zkflow.annotated.pilot.ivno.deps.Network
import com.ing.zkflow.annotated.pilot.r3.types.IssuedTokenType
import net.corda.core.contracts.Amount
// import com.ing.zkflow.annotated.pilot.r3.types.IssuedTokenType
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

object AnonymousPartyConverter_EdDSA : ConversionProvider<AnonymousParty, AnonymousPartySurrogate_EdDSA> {
    override fun from(original: AnonymousParty): AnonymousPartySurrogate_EdDSA {
        require(original.owningKey.algorithm == "EdDSA") {
            "This converter only accepts parties with EdDSA keys"
        }

        return AnonymousPartySurrogate_EdDSA(original.owningKey.encoded)
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
        }?.let { AnonymousPartyConverter_EdDSA.from(it) }

        return NetworkEdDSAAnonymousOperator(original.value, operator)
    }
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

object AmountConverter_IssuedTokenType : ConversionProvider<
        Amount<IssuedTokenType>,
        AmountSurrogate_IssuedTokenType
        > {
    override fun from(original: Amount<IssuedTokenType>) =
        AmountSurrogate_IssuedTokenType(original.quantity, original.displayTokenSize, original.token)
}

object CordaX500NameConverter : ConversionProvider<CordaX500Name, CordaX500NameSurrogate> {
    override fun from(original: CordaX500Name): CordaX500NameSurrogate =
        CordaX500NameSurrogate("$original")
}

object EdDSAPublicKeyConverter : ConversionProvider<PublicKey, EdDSAPublicKeySurrogate> {
    override fun from(original: PublicKey): EdDSAPublicKeySurrogate {
        require(original.encoded.size == 44) {
            "`${original.algorithm}` key encoding must be 44 bytes long; got ${original.encoded.size}"
        }

        // Ignore first 12 bytes.
        // For more information see Specs can be found in [X509EncodedKeySpec] (/java/security/spec/X509EncodedKeySpec.java).
        // The tail 32 bytes are the actual key.
        val key = original.encoded.copyOfRange(12, 44)
        require(key.size == 32) {
            "`${original.algorithm}` key must be 32 bytes long"
        }

        return EdDSAPublicKeySurrogate(key)
    }
}
