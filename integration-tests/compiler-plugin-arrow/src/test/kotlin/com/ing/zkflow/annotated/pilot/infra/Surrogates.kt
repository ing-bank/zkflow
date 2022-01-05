@file:Suppress("ClassName")

package com.ing.zkflow.annotated.pilot.infra

import com.ing.zkflow.Converter
import com.ing.zkflow.Default
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotated.pilot.ivno.IvnoTokenType
import com.ing.zkflow.annotated.pilot.ivno.deps.BigDecimalAmount
import com.ing.zkflow.annotated.pilot.ivno.deps.Network
import com.ing.zkflow.annotated.pilot.r3.types.IssuedTokenType
import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.util.UUID

@ZKP
@Suppress("ArrayInDataClass")
data class AnonymousPartySurrogate_EdDSA(
    val encodedEdDSA: @Size(44) ByteArray
) : Surrogate<AnonymousParty> {
    override fun toOriginal() = AnonymousParty(
        Crypto.decodePublicKey(Crypto.EDDSA_ED25519_SHA512, encodedEdDSA)
    )
}

@ZKP
@Suppress("ArrayInDataClass")
data class EdDSAParty(
    val cordaX500Name: @ASCII(50) String,
    val encodedEdDSA: @Size(44) ByteArray
) : Surrogate<Party> {
    override fun toOriginal() = Party(
        CordaX500Name.parse(cordaX500Name),
        Crypto.decodePublicKey(Crypto.EDDSA_ED25519_SHA512, encodedEdDSA)
    )
}

@ZKP
data class NetworkEdDSAAnonymousOperator(
    val value: @ASCII(10) String,
    val operator: @Default<AnonymousPartySurrogate_EdDSA>(EdDSAAnonymousPartyDefaultProvider::class) AnonymousPartySurrogate_EdDSA?
) : Surrogate<Network> {
    override fun toOriginal() = Network(value, operator?.toOriginal())
}

@ZKP
data class UniqueIdentifierSurrogate(
    val externalId: @ASCII(10) String?,
    val id: UUID
) : Surrogate<UniqueIdentifier> {
    override fun toOriginal() = UniqueIdentifier(externalId, id)
}

@ZKP
data class BigDecimalAmountSurrogate_LinearPointer_IvnoTokenType(
    val quantity: @BigDecimalSize(10, 10) BigDecimal,
    val amountType: @Converter<LinearPointer<IvnoTokenType>, LinearPointerSurrogate_IvnoTokenType>(
        LinearPointerConverter_IvnoTokenType::class
    ) LinearPointer<IvnoTokenType>
) : Surrogate<BigDecimalAmount<LinearPointer<IvnoTokenType>>> {
    override fun toOriginal() = BigDecimalAmount(quantity, amountType)
}

@ZKP
data class LinearPointerSurrogate_IvnoTokenType(
    val pointer: @Converter<UniqueIdentifier, UniqueIdentifierSurrogate>(UniqueIdentifierConverter::class) UniqueIdentifier,
    val className: @ASCII(100) String,
    val isResolved: Boolean
) : Surrogate<LinearPointer<IvnoTokenType>> {
    override fun toOriginal(): LinearPointer<IvnoTokenType> {
        @Suppress("UNCHECKED_CAST")
        val klass = Class.forName(className) as Class<IvnoTokenType>
        return LinearPointer(pointer, klass, isResolved)
    }
}

@ZKP
data class AmountSurrogate_IssuedTokenType(
    val quantity: Long,
    val displayTokenSize: @BigDecimalSize(10, 10) BigDecimal,
    val token: IssuedTokenType
) : Surrogate<Amount<IssuedTokenType>> {
    override fun toOriginal(): Amount<IssuedTokenType> = Amount(quantity, displayTokenSize, token)
}

@ZKP
@Suppress("ClassName")
data class CordaX500NameSurrogate(
    val concat: @ASCII(20) String
) : Surrogate<CordaX500Name> {
    override fun toOriginal(): CordaX500Name =
        CordaX500Name.parse(concat)
}
