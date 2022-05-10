package com.example.token.cbdc

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import net.corda.core.identity.AnonymousParty

// Open classes may be extended provided no values are overridden.
// Otherwise, serialization breaks.
// see, https://github.com/Kotlin/kotlinx.serialization/issues/1792

@ZKP
data class IssuedTokenType(
    val issuer: @EdDSA AnonymousParty,
    val tokenType: TokenType
) : TokenType(tokenType.tokenIdentifier, tokenType.fractionDigits)
